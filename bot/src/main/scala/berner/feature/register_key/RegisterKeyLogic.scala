package berner.feature.register_key

import berner.logging.Logger
import database.extension.UserPublicKeyExtension.{UserPublicKeyAlgorithm, UserPublicKeyType}
import org.bouncycastle.crypto.params.{Ed25519PublicKeyParameters, RSAKeyParameters}
import org.bouncycastle.crypto.util.{OpenSSHPublicKeyUtil, SubjectPublicKeyInfoFactory}
import org.bouncycastle.jcajce.interfaces.EdDSAPublicKey
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.openssl.PEMParser
import org.bouncycastle.openssl.jcajce.JcaPEMKeyConverter

import java.io.StringReader
import java.security.interfaces.RSAPublicKey
import java.security.spec.{RSAPublicKeySpec, X509EncodedKeySpec}
import java.security.{KeyFactory, PublicKey, Security}
import java.util.Base64
import scala.util.Using

object RegisterKeyLogic extends Logger {
  Security.addProvider(new BouncyCastleProvider())

  /**
   * 鍵のフォーマットを判別する
   */
  def detectKeyType(key: String): Either[RegisterKeyLogic.KeyParseError, UserPublicKeyType] = {
    val trimed = key.trim
    if (trimed.startsWith("-----BEGIN")) {
      Right(UserPublicKeyType.PEM)
    } else if (trimed.startsWith("ssh-")) {
      Right(UserPublicKeyType.OpenSSH)
    } else {
      Left(KeyParseError.UnsupportedFormat)
    }
  }

  /**
   * 鍵の文字列をパースする
   */
  def parseKey(key: String, keyType: UserPublicKeyType): Either[KeyParseError, (PublicKey, UserPublicKeyAlgorithm)] = {
    val publicKey = keyType match {
      case UserPublicKeyType.PEM => parsePEMKey(key)
      case UserPublicKeyType.OpenSSH => parseOpenSSHKey(key)
    }
    publicKey.flatMap {
      case key: RSAPublicKey => Right((key, UserPublicKeyAlgorithm.RSA))
      case key: EdDSAPublicKey => Right((key, UserPublicKeyAlgorithm.ECDSA))
      case _ => Left(KeyParseError.UnsupportedType)
    }
  }

  private def parsePEMKey(key: String): Either[RegisterKeyLogic.KeyParseError, PublicKey] = {
    Using.resource(new PEMParser(new StringReader(key))) { parser =>
      Option(parser.readObject()) match {
        case Some(info: org.bouncycastle.asn1.x509.SubjectPublicKeyInfo) =>
          Right(new JcaPEMKeyConverter().getPublicKey(info))
        case _ =>
          logger.warn("公開鍵のパースに失敗しました。")
          Left(KeyParseError.InvalidFormat)
      }
    }
  }

  private def parseOpenSSHKey(key: String): Either[KeyParseError, PublicKey] = {
    key.split(' ').toList match {
      case _ :: base64Key :: _ =>
        try {
          val keyBytes = Base64.getDecoder.decode(base64Key)
          OpenSSHPublicKeyUtil.parsePublicKey(keyBytes) match {
            case rsaKey: RSAKeyParameters =>
              val spec = new RSAPublicKeySpec(rsaKey.getModulus, rsaKey.getExponent)
              Right(KeyFactory.getInstance("RSA").generatePublic(spec))
            case ed25519Key: Ed25519PublicKeyParameters =>
              val subjectPublicKeyInfo = SubjectPublicKeyInfoFactory.createSubjectPublicKeyInfo(ed25519Key)
              val keyBytes = subjectPublicKeyInfo.getEncoded
              val spec = new X509EncodedKeySpec(keyBytes)
              Right(KeyFactory.getInstance("Ed25519", "BC").generatePublic(spec))
            case _ =>
              logger.warn(s"未サポートのアルゴリズムです。")
              Left(KeyParseError.UnsupportedType)
          }
        } catch {
          case e: Throwable =>
            logger.warn("公開鍵のパースに失敗しました。", e)
            Left(KeyParseError.InvalidFormat)
        }
      case _ => Left(KeyParseError.InvalidFormat)
    }
  }

  sealed abstract class KeyParseError()

  object KeyParseError {
    case object InvalidFormat extends KeyParseError
    case object UnsupportedFormat extends KeyParseError
    case object UnsupportedType extends KeyParseError
  }
}
