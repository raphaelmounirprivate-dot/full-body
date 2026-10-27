# AndroidViewModel est construit par la factory Android via son constructeur Application.
-keepclassmembers class * extends androidx.lifecycle.AndroidViewModel {
    public <init>(android.app.Application);
}
