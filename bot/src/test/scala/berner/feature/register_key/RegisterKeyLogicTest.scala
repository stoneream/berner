package berner.feature.register_key

import berner.model.register_key.UserPublicKey.UserPublicKeyType
import lib.TestBase

class RegisterKeyLogicTest extends TestBase {

  private val keyRsaOpenSSH =
    "ssh-rsa AAAAB3NzaC1yc2EAAAADAQABAAABAQCk7ay2/JCuZGNzqmGAFdHCgNYjIanS0p/YF0coceTlTaqdugtEVcVwSxKXhUEXVOGq0+gSO+lLmBPHZFsPMY/Qlkx7ZpyGaXfamRyQ7TsJOXKal9PADMv7FtIswTt4fdQl+5pKBATUwZbv4Rob0YwuoSOskHvKmHzAQMhh8oOIqMXzK2RO4yYXJhPft1vw7wozD6ChCNZaw1IVK1U86+z33lbGpVv08yWl3hTziD81mNpOyw8S9Xbm5VvpLVsiu3D7bUggwBBOpdJ6sXSILWlj6cnbHtE353Tg0QJkY3qGJ2qL676YjK22LjT0ihWFey8Z7c00ZqqOYzwKvTwpIM5n"
  private val keyEd25519OpenSSH = "ssh-ed25519 AAAAC3NzaC1lZDI1NTE5AAAAILcCqBaO5AI1dQZy21aDXjnZfK5xh08bCgml3BxP8LjV"
  private val keyPEM =
    """-----BEGIN PUBLIC KEY-----
      |MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAxvbCGRMGgGWjqGmmOkM4
      |E6cYJtlpN0dZfyKZ3O4SFpw3h1whAlGpQrtxxqm9zAkRD8FOz3QB70RwOE2NIbs1
      |vz54CutS9OOW8PZ5m5GzMx8mAlIASkPdmP4rAYnj/XJeDMaGIMzh63xJmeBLsSlL
      |vOvATesyxYZUvaClTbqQDgl9EnGXV3QamUJtILSTynm55/kC/mIoeCthRUO0TdPX
      |y4IeAeuAL5iOvx9ScJYV4a+nnADCmoIxCgPdg7ctRoCoBNjQTW4X5RcI95e5UZ7K
      |bDSWR0+9eCQbGkajOhqBb5WXj8+djWZ6JfR5oZQKnBCngb7gfg1McDRk5FemXXGN
      |oQIDAQAB
      |-----END PUBLIC KEY-----
      |""""".stripMargin

  test("鍵の形式を判定できる (PEM)") {
    val result = RegisterKeyLogic.detectKeyType(keyPEM)
    assert(result.contains(UserPublicKeyType.PEM))
  }

  test("鍵の型式を判定できる (OpenSSH)") {
    val result = RegisterKeyLogic.detectKeyType(keyRsaOpenSSH)
    assert(result.contains(UserPublicKeyType.OpenSSH))
  }

  test("鍵の型式が不正な場合にNoneを返す") {
    val key = "ajkdfakjdsfjaskdfakldjfalkj"
    val result = RegisterKeyLogic.detectKeyType(key)
    assert(result.isLeft)
  }

  test("鍵をパースできる (PEM)") {
    val result = RegisterKeyLogic.parseKey(keyPEM, UserPublicKeyType.PEM)
    assert(result.isRight)
  }

  test("鍵をパースできる (OpenSSH, RSA)") {
    val result = RegisterKeyLogic.parseKey(keyRsaOpenSSH, UserPublicKeyType.OpenSSH)
    assert(result.isRight)
  }

  test("鍵をパースできる (OpenSSH, Ed25519)") {
    val result = RegisterKeyLogic.parseKey(keyEd25519OpenSSH, UserPublicKeyType.OpenSSH)
    assert(result.isRight)
  }
}
