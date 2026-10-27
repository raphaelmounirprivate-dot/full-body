package be.moncoachsport

import androidx.room.*
import kotlinx.coroutines.flow.Flow
import org.json.*
import java.time.*

@Entity(tableName = "body_measurements")
data class BodyMeasurement(@PrimaryKey val day: String, val kg: Double?, val fat: Double?)

@Dao
interface MeasurementDao {
    @Query("SELECT * FROM body_measurements ORDER BY day") fun observe(): Flow<List<BodyMeasurement>>
    @Query("SELECT * FROM body_measurements ORDER BY day") suspend fun read(): List<BodyMeasurement>
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun put(value: BodyMeasurement)
    @Query("DELETE FROM body_measurements WHERE day = :day") suspend fun delete(day: String)
    @Query("DELETE FROM body_measurements") suspend fun clear()
}

data class PersonalOptions(
    val keepScreen: Boolean = true,
    val guided: Boolean = false,
    val cycleReminder: Boolean = false,
    val level: String = "Débutant",
    val homeOrder: List<String> = FeatureRules.homeCards,
    val hidden: Set<String> = emptySet(),
    val weeklyGoal: Int = 3,
    val challenges: Boolean = false,
    val videos: Map<String, String> = emptyMap(),
    val exerciseLevels: Map<String, String> = emptyMap(),
)

object PersonalCodec {
    fun encode(o: PersonalOptions) = JSONObject()
        .put("keepScreen", o.keepScreen).put("guided", o.guided).put("cycleReminder", o.cycleReminder)
        .put("level", o.level).put("homeOrder", JSONArray(o.homeOrder)).put("hidden", JSONArray(o.hidden.toList()))
        .put("weeklyGoal", o.weeklyGoal).put("challenges", o.challenges).put("videos", JSONObject(o.videos))
        .put("exerciseLevels", JSONObject(o.exerciseLevels))

    fun decode(j: JSONObject): PersonalOptions {
        fun strings(key: String) = j.getJSONArray(key).let { a -> (0 until a.length()).map { a.getString(it) } }
        val videos = j.getJSONObject("videos").let { v -> v.keys().asSequence().associateWith { v.getString(it) } }
        val o = PersonalOptions(j.getBoolean("keepScreen"), j.getBoolean("guided"), j.getBoolean("cycleReminder"),
            j.getString("level"), strings("homeOrder"), strings("hidden").toSet(),
            j.getInt("weeklyGoal"), j.getBoolean("challenges"), videos,
            j.optJSONObject("exerciseLevels")?.let { v -> v.keys().asSequence().associateWith { v.getString(it) } }.orEmpty())
        FeatureRules.validate(o)
        return o
    }
    fun measurements(values: List<BodyMeasurement>) = JSONArray().apply {
        values.forEach { put(JSONObject().put("day", it.day).put("kg", it.kg ?: JSONObject.NULL).put("fat", it.fat ?: JSONObject.NULL)) }
    }
    fun measurements(a: JSONArray): List<BodyMeasurement> {
        require(a.length() <= 10000) { "Trop de mesures corporelles" }
        val values = (0 until a.length()).map { i -> a.getJSONObject(i).let {
            BodyMeasurement(it.getString("day"), if (it.isNull("kg")) null else it.getDouble("kg"),
                if (it.isNull("fat")) null else it.getDouble("fat"))
        } }
        values.forEach { FeatureRules.validate(it) }
        require(values.map { it.day }.distinct().size == values.size) { "Dates de mesures dupliquées" }
        return values
    }
}
