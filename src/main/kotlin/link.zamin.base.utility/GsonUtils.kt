package link.zamin.base.utility

import com.google.gson.*
import org.bson.types.ObjectId
import java.lang.reflect.Type

object GsonUtils {
    private val gsonBuilder = GsonBuilder()
        .setDateFormat("yyyy-MM-dd'T'HH:mm:ssZ")
        .registerTypeAdapter(ObjectId::class.java, object : JsonSerializer<ObjectId?> {

            override fun serialize(src: ObjectId?, typeOfSrc: Type?, context: JsonSerializationContext?): JsonElement {
                return JsonPrimitive(src?.toHexString())
            }
        })
        .registerTypeAdapter(ObjectId::class.java, object : JsonDeserializer<ObjectId?> {
            @Throws(JsonParseException::class)
            override fun deserialize(
                json: JsonElement,
                typeOfT: Type?,
                context: JsonDeserializationContext?
            ): ObjectId {
                return ObjectId(json.asString)
            }
        })
    val gson: Gson
        get() = gsonBuilder.create()
}