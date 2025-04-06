package link.zamin.base.exceptions

//class Exceptions(override val message: String, val code: Int = 0) : RuntimeException(message)
class ThirdPartyServerException(override val message: String, val code: Int? = null) : RuntimeException(message)