
plugins {
    // Mantenha os seus aliases existentes.
    // Esta linha aplica o plugin 'com.android.application' usando a versão definida no seu ficheiro libs.versions.toml.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose) // Se estiver a usar Jetpack Compose

    // A linha abaixo era redundante porque o alias(libs.plugins.android.application) já declara e aplica o plugin.
    // Manter ambas as declarações causa o erro "Plugin with id 'com.android.application' was already requested".
    // id("com.android.application") version "8.9.1" apply false // REMOVIDA

}

    android {
    namespace = "com.example.aveirobus"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.aveirobus"
        minSdk = 30
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {

    implementation("androidx.compose.material3:material3:1.3.2") // Exemplo de versão, use a mais recente compatível
    implementation("androidx.activity:activity-compose:1.10.1") // Use a versão mais recente compatível
    implementation("com.google.code.gson:gson:2.11.0") // Mantida a versão mais recente do Gson
    implementation(libs.androidx.core.ktx) // Mantida a versão do catálogo para core-ktx
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.play.services.maps) // Certifique-se que este alias no libs.versions.toml aponta para uma versão válida de play-services-maps

    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)

    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.google.maps.android:maps-compose:4.3.3") // Verifique se esta é a versão mais recente compatível

    // As duas linhas abaixo são importantes e devem ser mantidas para a funcionalidade de mapas e localização
    implementation("com.google.android.gms:play-services-maps:19.2.0") // Exemplo de versão, verifique a mais recente compatível com o seu projeto. A sua era 19.2.0, o que é bom.
    implementation("com.google.android.gms:play-services-location:21.3.0") // Exemplo de versão, verifique a mais recente compatível. A sua era 21.0.1, o que é bom.

    // A linha implementation("com.google.android.gms:play-services-directions:18.0.0") FOI REMOVIDA
    // A linha implementation("androidx.core:core-ktx:1.15.0") FOI REMOVIDA (usar a do catálogo)
    // A linha implementation("com.google.code.gson:gson:2.10.1") FOI REMOVIDA (duplicada)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.8.0") // Ou a versão mais recente compatível
    implementation("androidx.compose.material:material-icons-extended-android:1.7.8") // Ou a versão mais recente compatível com a sua versão do Compose
    implementation(platform(libs.androidx.compose.bom)) // Certifique-se que o BOM está atualizado
    implementation("androidx.compose.material3:material3:1.3.2") // Ou uma versão mais recente compatível.


}
