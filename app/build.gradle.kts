plugins {
	id("com.android.application")
	id("org.jetbrains.kotlin.android")
}

android {
	namespace = "com.maydayalaska.openairsoftcountdown"
	compileSdk = 35

	defaultConfig {
		applicationId = "com.maydayalaska.openairsoftcountdown"
		minSdk = 23
		targetSdk = 35
		versionCode = 34
		versionName = "1.24"
	}

	compileOptions {
		sourceCompatibility = JavaVersion.VERSION_17
		targetCompatibility = JavaVersion.VERSION_17
	}

	kotlinOptions {
		jvmTarget = "17"
	}
}
