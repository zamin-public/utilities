package link.zamin.base.exceptions

class ThirdPartyServerException(override val message: String, val code: Int? = null) : RuntimeException(message)