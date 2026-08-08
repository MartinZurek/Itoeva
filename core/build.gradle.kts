plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
}

/**
 * Gemeinsamer Kern von :app (echte Glyph-Matrix-Hardware) und :app-sim (Simulator/Tamagotchi).
 *
 * Enthaelt bewusst NUR Datenschicht und Alarmplanung, keine UI und keine Darstellung: die beiden
 * Apps stellen eine faellige Erinnerung voellig unterschiedlich dar (Hardware-Matrix vs. Widget
 * und simulierte Matrix), und ihre Compose-Oberflaechen sind inzwischen auch inhaltlich
 * auseinandergelaufen. Der Uebergabepunkt ist reminder/ReminderHost.kt.
 *
 * minSdk 26 statt der 34 von :app - der Kern selbst braucht nichts Neueres, und :app-sim soll
 * auf moeglichst vielen Geraeten laufen. Das hoehere minSdk von :app bleibt dort bestehen.
 */
android {
    namespace = "com.notime.glyphcore"
    compileSdk = 35

    defaultConfig {
        minSdk = 26
        // Reicht die Keep-Regeln des Kerns an jede App weiter, die ihn einbindet - so muessen
        // :app und :app-sim nicht wissen, was der Kern intern reflektiv braucht.
        consumerProguardFiles("consumer-rules.pro")
    }

    // Fuer DefaultReminders: dort haengen die Test-Erinnerungen mit kurzen Intervallen am
    // Build-Typ, damit sie gar nicht erst in einem Release landen koennen.
    buildFeatures {
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    api(libs.androidx.room.runtime)
    api(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    api(libs.kotlinx.coroutines.android)
    // Der Watchdog gehoert in den Kern, weil beide Apps dieselbe Alarmkette teilen - api statt
    // implementation, damit die Apps den Lauf selbst einplanen koennen (siehe ReminderWatchdogWorker).
    api(libs.androidx.work.runtime.ktx)

    testImplementation(libs.junit)
}
