package com.maydayalaska.openairsoftcountdown

import android.Manifest
import android.annotation.SuppressLint
import android.app.Activity
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.BluetoothGattCallback
import android.bluetooth.BluetoothGattCharacteristic
import android.bluetooth.BluetoothGattDescriptor
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.ParcelUuid
import android.text.InputFilter
import android.text.InputType
import android.text.method.HideReturnsTransformationMethod
import android.text.method.PasswordTransformationMethod
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.inputmethod.InputMethodManager
import android.view.animation.AccelerateInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.WindowManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.nio.charset.Charset
import java.util.UUID

class MainActivity : Activity()
{
	private companion object
	{
		const val DeviceName = "Open Airsoft Countdown"
		const val PreferredMtu = 185
		const val PreferencesName = "open_airsoft_countdown_settings"
		const val ThemePreferenceKey = "theme_mode"
		const val LanguagePreferenceKey = "app_language"
		const val FirmwareRepositoryUrl = "https://github.com/MaydayAlaska/Open-Airsoft-Countdown"
		const val AndroidRepositoryUrl = "https://github.com/MaydayAlaska/Open-Airsoft-Countdown-Android-App"

		val ServiceUuid: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
		val CommandCharacteristicUuid: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
		val ResponseCharacteristicUuid: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
		val ClientConfigurationDescriptorUuid: UUID = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")
	}

	private enum class Screen
	{
		Main,
		Device,
		Config,
		Users,
		Settings
	}

	private enum class ThemeMode(val preferenceValue: String)
	{
		System("system"),
		Light("light"),
		Dark("dark")
	}

	private enum class AppLanguage(val preferenceValue: String)
	{
		Italian("it"),
		English("en")
	}

	private enum class ButtonStyle
	{
		Primary,
		Secondary,
		Success,
		Danger,
		Outline
	}

	private class AsteriskPasswordTransformationMethod : PasswordTransformationMethod()
	{
		override fun getTransformation(source: CharSequence, view: View): CharSequence
		{
			return AsteriskCharSequence(source)
		}

		private class AsteriskCharSequence(private val source: CharSequence) : CharSequence
		{
			override val length: Int
				get() = source.length

			override fun get(index: Int): Char
			{
				return '*'
			}

			override fun subSequence(startIndex: Int, endIndex: Int): CharSequence
			{
				return "*".repeat(endIndex - startIndex)
			}
		}
	}

	private data class AppPalette(
		val background: Int,
		val surface: Int,
		val surfaceAlt: Int,
		val textPrimary: Int,
		val textSecondary: Int,
		val accent: Int,
		val accentSoft: Int,
		val border: Int,
		val success: Int,
		val danger: Int,
		val warning: Int,
		val white: Int,
		val scrim: Int
	)

	private data class DeviceConfig(
		var adminPin: String = "000000",
		var bleName: String = DeviceName,
		var language: String = "it",
		var authorizedUserIds: String = "1",
		var soundEnabled: Boolean = true,
		var rfid: Boolean = false,
		var fingerprint: Boolean = false,
		var maxErrorCount: String = "3",
		var errorCountdownSeconds: String = "10"
	)

	private data class DeviceUser(
		val id: String,
		val name: String,
		val uid: String,
		val pin: String
	)

	private val handler = Handler(Looper.getMainLooper())
	private val foundDevices = linkedMapOf<String, BluetoothDevice>()

	private var bluetoothAdapter: BluetoothAdapter? = null
	private var bluetoothGatt: BluetoothGatt? = null
	private var commandCharacteristic: BluetoothGattCharacteristic? = null
	private var responseCharacteristic: BluetoothGattCharacteristic? = null
	private var isScanning = false
	private var isConnected = false
	private var selectedDeviceName = "--"
	private var currentScreen = Screen.Main
	private var loadUsersAfterConfig = false
	private var shouldOpenPinDialogAfterConnection = false
	private var pinDialogAlreadyShown = false
	private var pendingConfigRestartDialog = false
	private var isDrawerOpen = false
	private var themeMode = ThemeMode.System
	private var appLanguage = AppLanguage.Italian
	private var isDarkTheme = false
	private lateinit var palette: AppPalette
	private val commandButtons = mutableListOf<Button>()

	private val deviceConfig = DeviceConfig()
	private val deviceUsers = mutableListOf<DeviceUser>()
	private var expectedUserCount: Int? = null
	private var isReceivingUsers = false

	private var hasCachedStatus = false
	private var cachedRemainingSeconds = 0L
	private var cachedMode = "--"
	private var cachedErrors = "--"
	private var cachedLocked = false
	private var cachedLogged = false

	private lateinit var rootFrame: FrameLayout
	private lateinit var rootLayout: LinearLayout
	private lateinit var drawerScrim: View
	private lateinit var drawerLayout: LinearLayout
	private lateinit var titleLabel: TextView
	private lateinit var menuButton: Button
	private lateinit var statusLabel: TextView
	private lateinit var scrollView: ScrollView
	private lateinit var contentLayout: LinearLayout

	private var deviceListLayout: LinearLayout? = null
	private var selectedDeviceLabel: TextView? = null
	private var scanButton: Button? = null
	private var disconnectButton: Button? = null
	private var rawStatusLabel: TextView? = null
	private var timerLabel: TextView? = null
	private var modeLabel: TextView? = null
	private var errorsLabel: TextView? = null
	private var timeInput: EditText? = null

	private var configAdminPinInput: EditText? = null
	private var configBleNameInput: EditText? = null
	private var configLanguageGroup: RadioGroup? = null
	private var configLanguageItalianButton: RadioButton? = null
	private var configLanguageEnglishButton: RadioButton? = null
	private var configAuthorizedUserIdsInput: EditText? = null
	private var configSoundCheck: CheckBox? = null
	private var configRfidCheck: CheckBox? = null
	private var configFingerprintCheck: CheckBox? = null
	private var configMaxErrorInput: EditText? = null
	private var configErrorCountdownInput: EditText? = null
	private var usersSummaryLabel: TextView? = null
	private var usersListLayout: LinearLayout? = null

	private val scanCallback = object : ScanCallback()
	{
		override fun onScanResult(callbackType: Int, result: ScanResult)
		{
			val device = result.device
			val address = device.address ?: return
			val name = getDeviceName(device, result) ?: tr("Dispositivo BLE", "BLE device")

			if (!name.contains(DeviceName, ignoreCase = true) && result.scanRecord?.serviceUuids?.contains(ParcelUuid(ServiceUuid)) != true)
			{
				return
			}

			if (foundDevices.containsKey(address))
			{
				return
			}

			foundDevices[address] = device

			runOnUiThreadSafe {
				addDeviceButton(name, address, device)
			}
		}

		override fun onScanFailed(errorCode: Int)
		{
			runOnUiThreadSafe {
				showStatus(tr("Scansione fallita: $errorCode", "Scan failed: $errorCode"))
				isScanning = false
				updateVisibleButtons()
			}
		}
	}

	private val gattCallback = object : BluetoothGattCallback()
	{
		override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int)
		{
			runOnUiThreadSafe {
				if (newState == BluetoothProfile.STATE_CONNECTED)
				{
					isConnected = true
					showStatus(tr("Connesso. Richiesta MTU $PreferredMtu...", "Connected. Requesting MTU $PreferredMtu..."))
					updateVisibleButtons()

					if (!hasConnectPermission())
					{
						showStatus(tr("Permesso connessione Bluetooth mancante.", "Bluetooth connection permission is missing."))
						return@runOnUiThreadSafe
					}

					gatt.requestMtu(PreferredMtu)
					handler.postDelayed({ discoverServicesIfConnected() }, 1500)
				}
				else if (newState == BluetoothProfile.STATE_DISCONNECTED)
				{
					isConnected = false
					commandCharacteristic = null
					responseCharacteristic = null
					shouldOpenPinDialogAfterConnection = false
					pinDialogAlreadyShown = false
					selectedDeviceName = "--"
					loadUsersAfterConfig = false
					clearCachedStatus()
					updateSelectedDeviceLabel()
					showStatus(tr("Disconnesso.", "Disconnected."))
					if (currentScreen == Screen.Device)
					{
						renderCurrentScreen()
					}
					else
					{
						updateVisibleButtons()
					}
				}
			}
		}

		override fun onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int)
		{
			runOnUiThreadSafe {
				showStatus(tr("MTU impostato: $mtu. Ricerca servizi...", "MTU set to $mtu. Discovering services..."))
				discoverServicesIfConnected()
			}
		}

		override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int)
		{
			runOnUiThreadSafe {
				if (status != BluetoothGatt.GATT_SUCCESS)
				{
					showStatus(tr("Errore ricerca servizi: $status", "Service discovery error: $status"))
					return@runOnUiThreadSafe
				}

				val service = gatt.getService(ServiceUuid)

				if (service == null)
				{
					showStatus(tr("Servizio BLE non trovato.", "BLE service not found."))
					return@runOnUiThreadSafe
				}

				commandCharacteristic = service.getCharacteristic(CommandCharacteristicUuid)
				responseCharacteristic = service.getCharacteristic(ResponseCharacteristicUuid)

				if (commandCharacteristic == null || responseCharacteristic == null)
				{
					showStatus(tr("Characteristic BLE mancanti.", "Required BLE characteristics are missing."))
					return@runOnUiThreadSafe
				}

				enableNotifications(gatt, responseCharacteristic!!)
				showStatus(tr("Dispositivo pronto.", "Device ready."))
				updateVisibleButtons()
				sendCommand("STATUS")

				if (shouldOpenPinDialogAfterConnection && !pinDialogAlreadyShown)
				{
					pinDialogAlreadyShown = true
					showPinDialog()
				}
			}
		}

		override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic)
		{
			val value = characteristic.value ?: return
			handleBleResponse(String(value, Charset.forName("UTF-8")))
		}

		override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, value: ByteArray)
		{
			handleBleResponse(String(value, Charset.forName("UTF-8")))
		}
	}

	override fun onCreate(savedInstanceState: Bundle?)
	{
		appLanguage = loadAppLanguage()
		themeMode = loadThemeMode()
		isDarkTheme = resolveDarkTheme(themeMode)
		setTheme(if (isDarkTheme) R.style.AppThemeDark else R.style.AppThemeLight)

		super.onCreate(savedInstanceState)

		palette = createPalette(isDarkTheme)
		currentScreen = savedInstanceState?.getString("currentScreen")
			?.let { name -> Screen.entries.firstOrNull { it.name == name } }
			?: Screen.Main

		val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
		bluetoothAdapter = bluetoothManager.adapter

		buildBaseUi()
		renderCurrentScreen()
		handler.post { showStartupDialog() }
	}

	override fun onSaveInstanceState(outState: Bundle)
	{
		outState.putString("currentScreen", currentScreen.name)
		super.onSaveInstanceState(outState)
	}

	@Deprecated("Deprecated in Java")
	override fun onBackPressed()
	{
		if (isDrawerOpen)
		{
			closeDrawer()
			return
		}

		super.onBackPressed()
	}

	override fun onDestroy()
	{
		stopScan()
		disconnect()
		super.onDestroy()
	}

	private fun buildBaseUi()
	{
		configureSystemBars()
		isDrawerOpen = false

		rootFrame = FrameLayout(this)
		rootFrame.setBackgroundColor(palette.background)

		rootLayout = LinearLayout(this)
		rootLayout.orientation = LinearLayout.VERTICAL
		rootFrame.addView(rootLayout, frameMatch())

		val topBar = LinearLayout(this)
		topBar.orientation = LinearLayout.HORIZONTAL
		topBar.gravity = Gravity.CENTER_VERTICAL
		topBar.setPadding(dp(14), dp(10), dp(10), dp(10))
		topBar.background = roundedDrawable(palette.surface, 22, palette.border, 1)
		topBar.elevation = dp(3).toFloat()
		val topBarParams = matchWrap()
		topBarParams.setMargins(0, 0, 0, dp(10))
		rootLayout.addView(topBar, topBarParams)

		val brandBadge = ImageView(this)
		brandBadge.setImageResource(
			if (isDarkTheme)
			{
				R.drawable.title_logo_light
			}
			else
			{
				R.drawable.title_logo_dark
			}
		)
		brandBadge.scaleType = ImageView.ScaleType.FIT_CENTER
		topBar.addView(brandBadge, LinearLayout.LayoutParams(dp(42), dp(42)))

		val titleBlock = LinearLayout(this)
		titleBlock.orientation = LinearLayout.VERTICAL
		titleBlock.setPadding(dp(12), 0, dp(8), 0)
		topBar.addView(titleBlock, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

		titleLabel = TextView(this)
		titleLabel.text = "Open Airsoft Countdown"
		titleLabel.textSize = 18f
		titleLabel.setTextColor(palette.textPrimary)
		titleLabel.typeface = Typeface.DEFAULT_BOLD
		titleBlock.addView(titleLabel, matchWrap())

		val subtitle = TextView(this)
		subtitle.text = tr("Controller BLE", "BLE Controller")
		subtitle.textSize = 12f
		subtitle.setTextColor(palette.textSecondary)
		titleBlock.addView(subtitle, matchWrap())

		menuButton = Button(this)
		menuButton.text = "☰"
		menuButton.textSize = 22f
		menuButton.setTextColor(palette.textPrimary)
		menuButton.isAllCaps = false
		menuButton.minHeight = 0
		menuButton.minimumHeight = 0
		menuButton.minimumWidth = 0
		menuButton.setPadding(0, 0, 0, 0)
		menuButton.background = rippleDrawable(palette.surfaceAlt, palette.border, 14)
		menuButton.setOnClickListener { openDrawer() }
		topBar.addView(menuButton, LinearLayout.LayoutParams(dp(44), dp(44)))

		statusLabel = TextView(this)
		statusLabel.text = tr("Pronto.", "Ready.")
		statusLabel.textSize = 14f
		statusLabel.setTextColor(palette.textSecondary)
		statusLabel.setPadding(dp(14), dp(10), dp(14), dp(10))
		statusLabel.background = roundedDrawable(palette.surfaceAlt, 16)
		val statusParams = matchWrap()
		statusParams.setMargins(0, 0, 0, dp(12))
		rootLayout.addView(statusLabel, statusParams)

		scrollView = ScrollView(this)
		scrollView.isFillViewport = true
		scrollView.clipToPadding = false
		contentLayout = LinearLayout(this)
		contentLayout.orientation = LinearLayout.VERTICAL
		contentLayout.setPadding(0, 0, 0, dp(24))
		scrollView.addView(contentLayout)
		rootLayout.addView(scrollView, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

		drawerScrim = View(this)
		drawerScrim.setBackgroundColor(palette.scrim)
		drawerScrim.visibility = View.GONE
		drawerScrim.setOnClickListener { closeDrawer() }
		rootFrame.addView(drawerScrim, frameMatch())

		drawerLayout = LinearLayout(this)
		drawerLayout.orientation = LinearLayout.VERTICAL
		drawerLayout.setBackgroundColor(palette.surface)
		drawerLayout.elevation = dp(18).toFloat()
		drawerLayout.visibility = View.GONE
		rootFrame.addView(drawerLayout, drawerFrameParams())

		buildDrawerMenu()
		applySystemInsets()
		setContentView(rootFrame)
	}

	private fun configureSystemBars()
	{
		window.statusBarColor = palette.background
		window.navigationBarColor = palette.background

		var flags = window.decorView.systemUiVisibility

		flags = if (isDarkTheme)
		{
			flags and View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
		}
		else
		{
			flags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
		}

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
		{
			flags = if (isDarkTheme)
			{
				flags and View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR.inv()
			}
			else
			{
				flags or View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR
			}
		}

		window.decorView.systemUiVisibility = flags
	}

	private fun applySystemInsets()
	{
		rootFrame.setOnApplyWindowInsetsListener { _, insets ->
			val topInset: Int
			val bottomInset: Int

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R)
			{
				val bars = insets.getInsets(WindowInsets.Type.systemBars())
				topInset = bars.top
				bottomInset = bars.bottom
			}
			else
			{
				topInset = insets.systemWindowInsetTop
				bottomInset = insets.systemWindowInsetBottom
			}

			rootLayout.setPadding(dp(16), topInset + dp(12), dp(16), bottomInset + dp(12))
			drawerLayout.setPadding(dp(18), topInset + dp(18), dp(18), bottomInset + dp(18))

			insets
		}

		rootFrame.post { rootFrame.requestApplyInsets() }
	}

	private fun buildDrawerMenu()
	{
		drawerLayout.removeAllViews()

		val brand = LinearLayout(this)
		brand.orientation = LinearLayout.HORIZONTAL
		brand.gravity = Gravity.CENTER_VERTICAL
		brand.setPadding(dp(4), dp(2), dp(4), dp(20))
		drawerLayout.addView(brand, matchWrap())

		val badge = ImageView(this)
		badge.setImageResource(R.mipmap.ic_launcher)
		badge.scaleType = ImageView.ScaleType.FIT_CENTER
		brand.addView(badge, LinearLayout.LayoutParams(dp(42), dp(42)))

		val brandText = LinearLayout(this)
		brandText.orientation = LinearLayout.VERTICAL
		brandText.setPadding(dp(12), 0, 0, 0)
		brand.addView(brandText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

		val drawerTitle = TextView(this)
		drawerTitle.text = "Open Airsoft"
		drawerTitle.textSize = 19f
		drawerTitle.typeface = Typeface.DEFAULT_BOLD
		drawerTitle.setTextColor(palette.textPrimary)
		brandText.addView(drawerTitle, matchWrap())

		val drawerSubtitle = TextView(this)
		drawerSubtitle.text = tr("Controller countdown", "Countdown Controller")
		drawerSubtitle.textSize = 12f
		drawerSubtitle.setTextColor(palette.textSecondary)
		brandText.addView(drawerSubtitle, matchWrap())

		addDrawerItem(tr("Comandi", "Controls"), Screen.Main)
		addDrawerItem(tr("Dispositivo", "Device"), Screen.Device)
		addDrawerItem(tr("Configurazione", "Configuration"), Screen.Config)
		addDrawerItem(tr("Utenti", "Users"), Screen.Users)

		val divider = View(this)
		divider.setBackgroundColor(palette.border)
		val dividerParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(1))
		dividerParams.setMargins(dp(8), dp(12), dp(8), dp(12))
		drawerLayout.addView(divider, dividerParams)

		addDrawerItem(tr("Impostazioni", "Settings"), Screen.Settings)

		val spacer = View(this)
		drawerLayout.addView(spacer, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 0, 1f))

		val version = TextView(this)
		version.text = tr("Versione 1.24", "Version 1.24")
		version.textSize = 12f
		version.gravity = Gravity.CENTER_HORIZONTAL
		version.setTextColor(palette.textSecondary)
		version.setPadding(0, 0, 0, dp(10))
		drawerLayout.addView(version, matchWrap())

		val closeButton = createActionButton(tr("Chiudi menu", "Close menu"), ButtonStyle.Secondary, false) { closeDrawer() }
		drawerLayout.addView(closeButton, matchWrapWithTopMargin(0))
	}

	private fun addDrawerItem(text: String, screen: Screen)
	{
		val button = Button(this)
		button.text = text
		button.gravity = Gravity.CENTER_VERTICAL or Gravity.START
		button.textSize = 16f
		button.isAllCaps = false
		button.typeface = if (screen == currentScreen) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
		button.setTextColor(if (screen == currentScreen) palette.accent else palette.textPrimary)
		button.setPadding(dp(18), 0, dp(18), 0)
		button.background = rippleDrawable(
			if (screen == currentScreen) palette.accentSoft else Color.TRANSPARENT,
			if (screen == currentScreen) palette.accent else Color.TRANSPARENT,
			16
		)
		button.setOnClickListener {
			currentScreen = screen
			renderCurrentScreen()
			closeDrawer()
		}

		val params = matchWrap()
		params.height = dp(52)
		params.setMargins(0, 0, 0, dp(6))
		drawerLayout.addView(button, params)
	}

	private fun openDrawer()
	{
		if (isDrawerOpen)
		{
			return
		}

		isDrawerOpen = true
		buildDrawerMenu()
		drawerScrim.animate().cancel()
		drawerLayout.animate().cancel()
		drawerScrim.alpha = 0f
		drawerScrim.visibility = View.VISIBLE
		drawerScrim.animate().alpha(1f).setDuration(220).start()

		drawerLayout.visibility = View.VISIBLE
		drawerLayout.post {
			drawerLayout.translationX = drawerLayout.width.toFloat()
			drawerLayout.animate()
				.translationX(0f)
				.setDuration(300)
				.setInterpolator(DecelerateInterpolator())
				.start()
		}
	}

	private fun closeDrawer()
	{
		if (!isDrawerOpen)
		{
			return
		}

		isDrawerOpen = false
		drawerScrim.animate().cancel()
		drawerLayout.animate().cancel()

		val drawerWidth = if (drawerLayout.width > 0) drawerLayout.width.toFloat() else dp(310).toFloat()

		drawerLayout.animate()
			.translationX(drawerWidth)
			.setDuration(220)
			.setInterpolator(AccelerateInterpolator())
			.withEndAction {
				drawerLayout.visibility = View.GONE
				drawerLayout.translationX = 0f
			}
			.start()

		drawerScrim.animate()
			.alpha(0f)
			.setDuration(200)
			.withEndAction {
				drawerScrim.visibility = View.GONE
				drawerScrim.alpha = 1f
			}
			.start()
	}

	private fun showScreenMenu()
	{
		openDrawer()
	}

	private fun renderCurrentScreen()
	{
		contentLayout.removeAllViews()
		commandButtons.clear()
		deviceListLayout = null
		selectedDeviceLabel = null
		scanButton = null
		disconnectButton = null
		rawStatusLabel = null
		timerLabel = null
		modeLabel = null
		errorsLabel = null
		timeInput = null
		configAdminPinInput = null
		configBleNameInput = null
		configSoundCheck = null
		configRfidCheck = null
		configFingerprintCheck = null
		configMaxErrorInput = null
		configErrorCountdownInput = null
		usersSummaryLabel = null
		usersListLayout = null

		when (currentScreen)
		{
			Screen.Main -> renderMainScreen()
			Screen.Device -> renderDeviceScreen()
			Screen.Config -> renderConfigScreen()
			Screen.Users -> renderUsersScreen()
			Screen.Settings -> renderSettingsScreen()
		}

		updateVisibleButtons()
	}

	private fun renderMainScreen()
	{
		addPageHeader(tr("Comandi", "Controls"), tr("Controlla il countdown e ricevi lo stato in tempo reale.", "Control the countdown and receive real-time status updates."))

		val timerCard = addCard()
		modeLabel = TextView(this)
		modeLabel!!.text = tr("Stato: --", "Status: --")
		modeLabel!!.textSize = 14f
		modeLabel!!.setTextColor(palette.textSecondary)
		modeLabel!!.gravity = Gravity.CENTER_HORIZONTAL
		timerCard.addView(modeLabel, matchWrap())

		timerLabel = TextView(this)
		timerLabel!!.text = "--:--"
		timerLabel!!.textSize = 54f
		timerLabel!!.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
		timerLabel!!.setTextColor(palette.textPrimary)
		timerLabel!!.gravity = Gravity.CENTER
		timerLabel!!.setPadding(0, dp(8), 0, dp(8))
		timerCard.addView(timerLabel, matchWrap())

		errorsLabel = TextView(this)
		errorsLabel!!.text = tr("Errori: --", "Errors: --")
		errorsLabel!!.textSize = 14f
		errorsLabel!!.setTextColor(palette.textSecondary)
		errorsLabel!!.gravity = Gravity.CENTER_HORIZONTAL
		timerCard.addView(errorsLabel, matchWrap())

		val timeCard = addCard()
		addCardTitle(timeCard, tr("Imposta durata", "Set duration"))
		addCardDescription(timeCard, tr("Inserisci il tempo nel formato HHMMSS. Esempio: 000030 per 30 secondi.", "Enter the time in HHMMSS format. Example: 000030 for 30 seconds."))
		timeInput = addStyledEditText(timeCard, tr("Tempo HHMMSS", "Time HHMMSS"), InputType.TYPE_CLASS_NUMBER)
		timeInput!!.filters = arrayOf(InputFilter.LengthFilter(6))

		val setTimeButton = createActionButton(tr("Imposta tempo", "Set time"), ButtonStyle.Primary, true) {
			val duration = timeInput!!.text.toString().trim()

			if (duration.length != 6)
			{
				showStatus(
					tr(
						"La durata deve contenere esattamente 6 cifre nel formato HHMMSS.",
						"The duration must contain exactly 6 digits in HHMMSS format."
					)
				)
				return@createActionButton
			}

			sendCommand("SETTIME:$duration")
		}
		timeCard.addView(setTimeButton, matchWrapWithTopMargin(10))

		val controlsCard = addCard()
		addCardTitle(controlsCard, tr("Controlli", "Controls"))
		val firstRow = createButtonRow()
		firstRow.addView(createActionButton("START", ButtonStyle.Success, true) { sendCommand("START") }, weightedButtonParams(false))
		firstRow.addView(createActionButton("STOP", ButtonStyle.Danger, true) { sendCommand("STOP") }, weightedButtonParams(true))
		controlsCard.addView(firstRow, matchWrapWithTopMargin(10))

		val secondRow = createButtonRow()
		secondRow.addView(createActionButton("RESET", ButtonStyle.Secondary, true) { sendCommand("RESET") }, weightedButtonParams(false))
		secondRow.addView(createActionButton("PING", ButtonStyle.Outline, true) { sendCommand("PING") }, weightedButtonParams(true))
		controlsCard.addView(secondRow, matchWrapWithTopMargin(8))

		val logCard = addCard()
		addCardTitle(logCard, tr("Ultima risposta BLE", "Latest BLE response"))
		rawStatusLabel = TextView(this)
		rawStatusLabel!!.text = tr("Nessuna notifica BLE ricevuta.", "No BLE notification received.")
		rawStatusLabel!!.textSize = 13f
		rawStatusLabel!!.setTextColor(palette.textSecondary)
		rawStatusLabel!!.typeface = Typeface.MONOSPACE
		rawStatusLabel!!.setPadding(0, dp(8), 0, 0)
		logCard.addView(rawStatusLabel, matchWrap())

		applyCachedStatusToMainScreen()
	}

	private fun renderDeviceScreen()
	{
		addPageHeader(tr("Dispositivo", "Device"), tr("Cerca, collega e autentica il controller ESP32.", "Find, connect to, and authenticate the ESP32 controller."))

		val connectionCard = addCard()
		addCardTitle(connectionCard, tr("Connessione", "Connection"))
		selectedDeviceLabel = TextView(this)
		selectedDeviceLabel!!.textSize = 16f
		selectedDeviceLabel!!.setTextColor(palette.textPrimary)
		selectedDeviceLabel!!.setPadding(0, dp(8), 0, dp(10))
		connectionCard.addView(selectedDeviceLabel, matchWrap())
		updateSelectedDeviceLabel()

		scanButton = createActionButton(tr("Scansiona dispositivo", "Scan for device"), ButtonStyle.Primary, false) { startScan() }
		connectionCard.addView(scanButton, matchWrapWithTopMargin(4))

		disconnectButton = createActionButton(tr("Disconnetti", "Disconnect"), ButtonStyle.Danger, false) { disconnect() }
		connectionCard.addView(disconnectButton, matchWrapWithTopMargin(8))
		connectionCard.addView(createActionButton(tr("Richiedi stato", "Request status"), ButtonStyle.Outline, true) { sendCommand("STATUS") }, matchWrapWithTopMargin(8))

		addSectionLabel(tr("Dispositivi trovati", "Devices found"))
		deviceListLayout = LinearLayout(this)
		deviceListLayout!!.orientation = LinearLayout.VERTICAL
		contentLayout.addView(deviceListLayout, matchWrap())

		for ((address, device) in foundDevices)
		{
			val name = if (hasConnectPermission()) device.name ?: tr("Dispositivo BLE", "BLE device") else tr("Dispositivo BLE", "BLE device")
			addDeviceButton(name, address, device)
		}
	}

	private fun renderConfigScreen()
	{
		addPageHeader(tr("Configurazione", "Configuration"), tr("Leggi e modifica le impostazioni salvate nel config.json.", "Read and edit the settings stored in config.json."))

		val identityCard = addCard()
		addCardTitle(identityCard, tr("Identità e accesso", "Identity and access"))
		addFieldLabel(identityCard, tr("PIN Admin", "Admin PIN"))
		configAdminPinInput = addStyledEditText(identityCard, tr("Inserisci il PIN amministratore", "Enter the administrator PIN"), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
		configAdminPinInput!!.setText(deviceConfig.adminPin)
		addFieldLabel(identityCard, tr("Nome del dispositivo", "Device name"))
		configBleNameInput = addStyledEditText(identityCard, tr("Inserisci il nome Bluetooth/BLE", "Enter the Bluetooth/BLE name"), InputType.TYPE_CLASS_TEXT)
		configBleNameInput!!.setText(deviceConfig.bleName)

		val deviceLanguageCard = addCard()
		addCardTitle(deviceLanguageCard, tr("Lingua del dispositivo", "Device language"))
		addCardDescription(
			deviceLanguageCard,
			tr(
				"Seleziona la lingua dei messaggi mostrati sul display OLED.",
				"Select the language used for messages on the OLED display."
			)
		)

		configLanguageGroup = RadioGroup(this)
		configLanguageGroup!!.orientation = RadioGroup.VERTICAL
		configLanguageGroup!!.setPadding(0, dp(6), 0, 0)

		configLanguageItalianButton = createSelectionRadioButton("Italiano")
		configLanguageEnglishButton = createSelectionRadioButton("English")

		configLanguageGroup!!.addView(configLanguageItalianButton)
		configLanguageGroup!!.addView(configLanguageEnglishButton)
		deviceLanguageCard.addView(configLanguageGroup, matchWrap())

		if (deviceConfig.language == "en")
		{
			configLanguageEnglishButton!!.isChecked = true
		}
		else
		{
			configLanguageItalianButton!!.isChecked = true
		}

		val authorizedUsersCard = addCard()
		addCardTitle(authorizedUsersCard, tr("Utenti autorizzati", "Authorized users"))
		addFieldLabel(authorizedUsersCard, tr("ID utenti autorizzati", "Authorized user IDs"))
		configAuthorizedUserIdsInput = addStyledEditText(
			authorizedUsersCard,
			tr("Esempio: 1;2 oppure 1,2", "Example: 1;2 or 1,2"),
			InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS
		)
		configAuthorizedUserIdsInput!!.filters = arrayOf(InputFilter.LengthFilter(32))
		configAuthorizedUserIdsInput!!.setText(deviceConfig.authorizedUserIds)

		addCardDescription(
			authorizedUsersCard,
			tr(
				"Usa il punto e virgola (;) se basta uno qualsiasi degli utenti indicati. Usa la virgola (,) se devono autenticarsi tutti, in qualsiasi ordine, fino a un massimo di 4 utenti. Non mescolare i due separatori.",
				"Use a semicolon (;) when any listed user may stop the timer. Use a comma (,) when every listed user must authenticate, in any order, up to a maximum of 4 users. Do not mix the two separators."
			)
		)

		val modulesCard = addCard()
		addCardTitle(modulesCard, tr("Moduli", "Modules"))
		configSoundCheck = addStyledCheckBox(modulesCard, tr("Buzzer attivo", "Buzzer enabled"), deviceConfig.soundEnabled)
		configRfidCheck = addStyledCheckBox(modulesCard, tr("Lettore RFID/NFC", "RFID/NFC reader"), deviceConfig.rfid)
		configFingerprintCheck = addStyledCheckBox(modulesCard, tr("Lettore impronte", "Fingerprint reader"), deviceConfig.fingerprint)

		val securityCard = addCard()
		addCardTitle(securityCard, tr("Errori e penalità", "Errors and penalties"))
		addFieldLabel(securityCard, tr("Numero errori massimo", "Maximum error count"))
		configMaxErrorInput = addStyledEditText(securityCard, tr("Da 1 a 10", "From 1 to 10"), InputType.TYPE_CLASS_NUMBER)
		configMaxErrorInput!!.setText(deviceConfig.maxErrorCount)
		addFieldLabel(securityCard, tr("Tempo ridotto a: (HHMMSS. Esempio: 000030 = 30 secondi)", "Reduced time: (HHMMSS. Example: 000030 = 30 seconds)"))
		configErrorCountdownInput = addStyledEditText(securityCard, tr("000000 disattiva la riduzione", "000000 disables time reduction"), InputType.TYPE_CLASS_NUMBER)
		configErrorCountdownInput!!.setText(formatSecondsAsHhmmss(deviceConfig.errorCountdownSeconds))

		val actionsCard = addCard()
		addCardTitle(actionsCard, tr("Sincronizzazione", "Synchronization"))
		actionsCard.addView(createActionButton(tr("Leggi config dal dispositivo", "Read config from device"), ButtonStyle.Outline, true) {
			showStatus(tr("Lettura configurazione...", "Reading configuration..."))
			sendCommand("GETCONFIG")
		}, matchWrapWithTopMargin(8))

		actionsCard.addView(createActionButton(tr("Salva config sul dispositivo", "Save config to device"), ButtonStyle.Primary, true) {
			val adminPin = configAdminPinInput!!.text.toString().trim()
			val bleName = configBleNameInput!!.text.toString().trim()
			val maxErrorCount = configMaxErrorInput!!.text.toString().trim()
			val errorCountdownText = configErrorCountdownInput!!.text.toString().trim()
			val authorizedUserIds = configAuthorizedUserIdsInput!!.text.toString()
			val deviceLanguage =
				if (configLanguageEnglishButton?.isChecked == true) "en" else "it"

			if (adminPin.length != 6 || adminPin.any { !it.isDigit() })
			{
				showStatus(tr("Il PIN amministratore deve contenere esattamente 6 cifre.", "The administrator PIN must contain exactly 6 digits."))
				return@createActionButton
			}

			if (bleName.isEmpty() || bleName.contains(';') || bleName.contains('='))
			{
				showStatus(tr("Nome Bluetooth/BLE non valido.", "Invalid Bluetooth/BLE name."))
				return@createActionButton
			}

			val authorizedUserIdsError = validateAuthorizedUserIds(authorizedUserIds)
			if (authorizedUserIdsError != null)
			{
				showStatus(authorizedUserIdsError)
				return@createActionButton
			}

			val maxErrorValue = maxErrorCount.toIntOrNull()
			if (maxErrorValue == null || maxErrorValue !in 1..10)
			{
				showStatus(tr("Il numero massimo di errori deve essere compreso tra 1 e 10.", "The maximum error count must be between 1 and 10."))
				return@createActionButton
			}

			val errorCountdownValue = parseHhmmssToSeconds(errorCountdownText)
			if (errorCountdownValue == null || errorCountdownValue !in 0..3600)
			{
				showStatus(tr("Tempo ridotto non valido. Usa HHMMSS, da 000000 a 010000.", "Invalid reduced time. Use HHMMSS, from 000000 to 010000."))
				return@createActionButton
			}

			val command = "SETCONFIG:" +
				"adminPin=$adminPin;" +
				"bleName=$bleName;" +
				"language=$deviceLanguage;" +
				"authorizedUserIds=$authorizedUserIds;" +
				"soundEnabled=${if (configSoundCheck!!.isChecked) 1 else 0};" +
				"rfid=${if (configRfidCheck!!.isChecked) 1 else 0};" +
				"fingerprint=${if (configFingerprintCheck!!.isChecked) 1 else 0};" +
				"maxErrorCount=$maxErrorCount;" +
				"errorCountdownSeconds=$errorCountdownValue"

			showStatus(tr("Salvataggio configurazione...", "Saving configuration..."))
			pendingConfigRestartDialog = true
			sendCommand(command)
		}, matchWrapWithTopMargin(8))
	}

	private fun renderUsersScreen()
	{
		addPageHeader(tr("Utenti", "Users"), tr("Gestisci gli utenti salvati nel file users.json.", "Manage the users stored in users.json."))

		val addCard = addCard()
		addCardTitle(addCard, tr("Nuovo utente", "New user"))
		val userNameInput = addStyledEditText(addCard, tr("Nome utente", "User name"), InputType.TYPE_CLASS_TEXT)
		val userUidInput = addStyledEditText(addCard, "UID NFC", InputType.TYPE_CLASS_TEXT)
		val userPinInput = addStyledEditText(addCard, tr("PIN utente (6 cifre)", "User PIN (6 digits)"), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
		configureSixDigitPinInput(userPinInput)

		addCard.addView(createActionButton(tr("Aggiungi utente", "Add user"), ButtonStyle.Primary, true) {
			val name = userNameInput.text.toString().trim()
			val uid = userUidInput.text.toString().trim().uppercase()
			val pin = userPinInput.text.toString().trim()

			if (!isValidUserName(name))
			{
				showStatus(tr("Nome utente non valido.", "Invalid user name."))
				return@createActionButton
			}

			if (!isValidUserUid(uid))
			{
				showStatus(tr("UID NFC non valido.", "Invalid NFC UID."))
				return@createActionButton
			}

			if (!isValidSixDigitPin(pin))
			{
				showStatus(tr("Il PIN utente deve contenere esattamente 6 cifre.", "The user PIN must contain exactly 6 digits."))
				return@createActionButton
			}

			showStatus(tr("Aggiunta utente...", "Adding user..."))
			sendCommand("ADDUSER:name=$name;uid=$uid;pin=$pin")
		}, matchWrapWithTopMargin(10))

		val listCard = addCard()
		val listHeader = createButtonRow()
		val headerText = TextView(this)
		headerText.text = tr("Utenti sul dispositivo", "Users on device")
		headerText.textSize = 18f
		headerText.typeface = Typeface.DEFAULT_BOLD
		headerText.setTextColor(palette.textPrimary)
		listHeader.addView(headerText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

		val readUsers = createActionButton(tr("Aggiorna", "Refresh"), ButtonStyle.Outline, true) {
			showStatus(tr("Lettura utenti...", "Reading users..."))
			sendCommand("GETUSERS")
		}
		listHeader.addView(readUsers, LinearLayout.LayoutParams(dp(110), dp(46)))
		listCard.addView(listHeader, matchWrap())

		usersSummaryLabel = TextView(this)
		usersSummaryLabel!!.textSize = 14f
		usersSummaryLabel!!.setTextColor(palette.textSecondary)
		usersSummaryLabel!!.setPadding(0, dp(10), 0, dp(8))
		listCard.addView(usersSummaryLabel, matchWrap())

		usersListLayout = LinearLayout(this)
		usersListLayout!!.orientation = LinearLayout.VERTICAL
		listCard.addView(usersListLayout, matchWrap())
		updateUsersViews()
	}

	private fun renderSettingsScreen()
	{
		addPageHeader(tr("Impostazioni", "Settings"), tr("Personalizza l'aspetto e la lingua dell'applicazione.", "Customize the application appearance and language."))

		val themeCard = addCard()
		addCardTitle(themeCard, tr("Tema", "Theme"))
		addCardDescription(themeCard, tr("Scegli il tema dell'app. Con Sistema, l'aspetto segue automaticamente Android.", "Choose the app theme. With System, the appearance automatically follows Android."))

		val group = RadioGroup(this)
		group.orientation = RadioGroup.VERTICAL
		group.setPadding(0, dp(6), 0, 0)
		val systemButton = createThemeRadioButton(tr("Sistema", "System"), tr("Segue il tema impostato su Android", "Follows the theme selected in Android"))
		val lightButton = createThemeRadioButton(tr("Chiaro", "Light"), tr("Interfaccia luminosa", "Light interface"))
		val darkButton = createThemeRadioButton(tr("Scuro", "Dark"), tr("Interfaccia scura", "Dark interface"))
		group.addView(systemButton)
		group.addView(lightButton)
		group.addView(darkButton)
		themeCard.addView(group, matchWrap())

		when (themeMode)
		{
			ThemeMode.System -> systemButton.isChecked = true
			ThemeMode.Light -> lightButton.isChecked = true
			ThemeMode.Dark -> darkButton.isChecked = true
		}

		group.setOnCheckedChangeListener { _, checkedId ->
			val selected = when (checkedId)
			{
				systemButton.id -> ThemeMode.System
				lightButton.id -> ThemeMode.Light
				darkButton.id -> ThemeMode.Dark
				else -> themeMode
			}

			if (selected != themeMode)
			{
				applyThemeMode(selected)
			}
		}

		val languageCard = addCard()
		addCardTitle(languageCard, tr("Lingua", "Language"))
		addCardDescription(languageCard, tr("Scegli la lingua dell'applicazione.", "Choose the application language."))

		val languageGroup = RadioGroup(this)
		languageGroup.orientation = RadioGroup.VERTICAL
		languageGroup.setPadding(0, dp(6), 0, 0)
		val italianButton = createSelectionRadioButton("Italiano")
		val englishButton = createSelectionRadioButton("English")
		languageGroup.addView(italianButton)
		languageGroup.addView(englishButton)
		languageCard.addView(languageGroup, matchWrap())

		when (appLanguage)
		{
			AppLanguage.Italian -> italianButton.isChecked = true
			AppLanguage.English -> englishButton.isChecked = true
		}

		languageGroup.setOnCheckedChangeListener { _, checkedId ->
			val selected = when (checkedId)
			{
				italianButton.id -> AppLanguage.Italian
				englishButton.id -> AppLanguage.English
				else -> appLanguage
			}

			if (selected != appLanguage)
			{
				applyAppLanguage(selected)
			}
		}

		val infoCard = addCard()
		addCardTitle(infoCard, tr("Informazioni", "Information"))
		addCenteredInfoLogo(infoCard)
		addInfoRow(infoCard, tr("Versione", "Version"), "1.24")
		addInfoRow(infoCard, tr("Tema attivo", "Active theme"), if (isDarkTheme) tr("Scuro", "Dark") else tr("Chiaro", "Light"))

		addCardDescription(infoCard, tr("Puoi trovare gli aggiornamenti qui:", "You can find updates here:"))
		addCardDescription(infoCard, tr("Repository GitHub", "GitHub repositories"))
		addGitHubLinkRow(
			infoCard,
			tr("Progetto Open Airsoft Countdown", "Open Airsoft Countdown project"),
			tr("Firmware ESP32, hardware e documentazione", "ESP32 firmware, hardware, and documentation"),
			FirmwareRepositoryUrl
		)
		addGitHubLinkRow(
			infoCard,
			tr("Applicazione Android", "Android application"),
			tr("Codice sorgente dell'applicazione", "Application source code"),
			AndroidRepositoryUrl
		)
	}

	private fun createThemeRadioButton(title: String, description: String): RadioButton
	{
		val button = RadioButton(this)
		button.id = View.generateViewId()
		button.text = "$title\n$description"
		button.textSize = 15f
		button.setTextColor(palette.textPrimary)
		button.buttonTintList = ColorStateList.valueOf(palette.accent)
		button.setPadding(dp(4), dp(10), dp(4), dp(10))
		return button
	}

	private fun createSelectionRadioButton(title: String): RadioButton
	{
		val button = RadioButton(this)
		button.id = View.generateViewId()
		button.text = title
		button.textSize = 15f
		button.setTextColor(palette.textPrimary)
		button.buttonTintList = ColorStateList.valueOf(palette.accent)
		button.setPadding(dp(4), dp(10), dp(4), dp(10))
		return button
	}

	private fun addPageHeader(title: String, subtitle: String)
	{
		val titleView = TextView(this)
		titleView.text = title
		titleView.textSize = 28f
		titleView.typeface = Typeface.DEFAULT_BOLD
		titleView.setTextColor(palette.textPrimary)
		contentLayout.addView(titleView, matchWrap())

		val subtitleView = TextView(this)
		subtitleView.text = subtitle
		subtitleView.textSize = 15f
		subtitleView.setTextColor(palette.textSecondary)
		subtitleView.setPadding(0, dp(4), 0, dp(18))
		contentLayout.addView(subtitleView, matchWrap())
	}

	private fun addSectionTitle(text: String)
	{
		addPageHeader(text, "")
	}

	private fun addSectionLabel(text: String)
	{
		val label = TextView(this)
		label.text = text
		label.textSize = 17f
		label.typeface = Typeface.DEFAULT_BOLD
		label.setTextColor(palette.textPrimary)
		label.setPadding(dp(4), dp(8), 0, dp(10))
		contentLayout.addView(label, matchWrap())
	}

	private fun addInfoText(text: String)
	{
		val label = TextView(this)
		label.text = text
		label.textSize = 15f
		label.setTextColor(palette.textSecondary)
		label.setPadding(0, 0, 0, dp(14))
		contentLayout.addView(label, matchWrap())
	}

	private fun addCard(): LinearLayout
	{
		val card = LinearLayout(this)
		card.orientation = LinearLayout.VERTICAL
		card.setPadding(dp(16), dp(16), dp(16), dp(16))
		card.background = roundedDrawable(palette.surface, 22, palette.border, 1)
		card.elevation = dp(2).toFloat()
		val params = matchWrap()
		params.setMargins(0, 0, 0, dp(14))
		contentLayout.addView(card, params)
		return card
	}

	private fun addCardTitle(parent: LinearLayout, text: String)
	{
		val label = TextView(this)
		label.text = text
		label.textSize = 18f
		label.typeface = Typeface.DEFAULT_BOLD
		label.setTextColor(palette.textPrimary)
		parent.addView(label, matchWrap())
	}

	private fun addCardDescription(parent: LinearLayout, text: String)
	{
		val label = TextView(this)
		label.text = text
		label.textSize = 13f
		label.setTextColor(palette.textSecondary)
		label.setPadding(0, dp(4), 0, dp(8))
		parent.addView(label, matchWrap())
	}

	private fun addCenteredInfoLogo(parent: LinearLayout)
	{
		val wrapper = LinearLayout(this)
		wrapper.orientation = LinearLayout.VERTICAL
		wrapper.gravity = Gravity.CENTER_HORIZONTAL
		wrapper.setPadding(0, dp(8), 0, dp(12))

		val logo = ImageView(this)
		logo.setImageResource(
			if (isDarkTheme)
			{
				R.drawable.info_logo_dark
			}
			else
			{
				R.drawable.info_logo_light
			}
		)
		logo.adjustViewBounds = true
		logo.scaleType = ImageView.ScaleType.FIT_CENTER
		wrapper.addView(logo, LinearLayout.LayoutParams(dp(120), dp(120)))

		parent.addView(wrapper, matchWrap())
	}

	private fun addInfoRow(parent: LinearLayout, labelText: String, valueText: String)
	{
		val row = createButtonRow()
		row.setPadding(0, dp(8), 0, dp(8))
		val label = TextView(this)
		label.text = labelText
		label.textSize = 14f
		label.setTextColor(palette.textSecondary)
		row.addView(label, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
		val value = TextView(this)
		value.text = valueText
		value.textSize = 14f
		value.typeface = Typeface.DEFAULT_BOLD
		value.setTextColor(palette.textPrimary)
		row.addView(value, LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT))
		parent.addView(row, matchWrap())
	}

	private fun addGitHubLinkRow(parent: LinearLayout, title: String, description: String, url: String)
	{
		val row = LinearLayout(this)
		row.orientation = LinearLayout.HORIZONTAL
		row.gravity = Gravity.CENTER_VERTICAL
		row.setPadding(dp(10), dp(10), dp(10), dp(10))
		row.background = rippleDrawable(palette.surfaceAlt, palette.border, 15)
		row.isClickable = true
		row.isFocusable = true
		row.setOnClickListener { openUrl(url) }

		val icon = ImageButton(this)
		icon.setImageResource(R.drawable.ic_github)
		icon.imageTintList = ColorStateList.valueOf(palette.textPrimary)
		icon.background = null
		icon.contentDescription = tr("Apri repository GitHub", "Open GitHub repository")
		icon.setPadding(dp(8), dp(8), dp(8), dp(8))
		icon.setOnClickListener { openUrl(url) }
		row.addView(icon, LinearLayout.LayoutParams(dp(48), dp(48)))

		val textBlock = LinearLayout(this)
		textBlock.orientation = LinearLayout.VERTICAL
		textBlock.setPadding(dp(8), 0, 0, 0)

		val titleView = TextView(this)
		titleView.text = title
		titleView.textSize = 14f
		titleView.typeface = Typeface.DEFAULT_BOLD
		titleView.setTextColor(palette.textPrimary)
		textBlock.addView(titleView, matchWrap())

		val descriptionView = TextView(this)
		descriptionView.text = description
		descriptionView.textSize = 12f
		descriptionView.setTextColor(palette.textSecondary)
		textBlock.addView(descriptionView, matchWrap())

		row.addView(textBlock, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

		val arrow = TextView(this)
		arrow.text = "›"
		arrow.textSize = 26f
		arrow.setTextColor(palette.textSecondary)
		arrow.gravity = Gravity.CENTER
		row.addView(arrow, LinearLayout.LayoutParams(dp(30), dp(48)))

		val params = matchWrap()
		params.setMargins(0, dp(8), 0, 0)
		parent.addView(row, params)
	}

	private fun openUrl(url: String)
	{
		try
		{
			startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
		}
		catch (error: Exception)
		{
			showStatus(tr("Impossibile aprire il link.", "Unable to open the link."))
		}
	}

	private fun addFieldLabel(parent: LinearLayout, text: String)
	{
		val label = TextView(this)
		label.text = text
		label.textSize = 12f
		label.typeface = Typeface.DEFAULT_BOLD
		label.setTextColor(palette.textSecondary)
		val params = matchWrap()
		params.setMargins(dp(2), dp(14), dp(2), 0)
		parent.addView(label, params)
	}

	private fun addStyledEditText(parent: LinearLayout, hint: String, inputType: Int): EditText
	{
		val editText = EditText(this)
		editText.hint = hint
		editText.inputType = inputType
		editText.setSingleLine(true)
		editText.setTextColor(palette.textPrimary)
		editText.setHintTextColor(palette.textSecondary)
		editText.textSize = 15f
		editText.setPadding(dp(14), 0, dp(14), 0)
		editText.background = roundedDrawable(palette.surfaceAlt, 15, palette.border, 1)
		val params = matchWrap()
		params.height = dp(52)
		params.setMargins(0, dp(8), 0, 0)
		parent.addView(editText, params)
		return editText
	}

	private fun addEditText(hint: String, inputType: Int): EditText
	{
		return addStyledEditText(contentLayout, hint, inputType)
	}

	private fun addStyledCheckBox(parent: LinearLayout, text: String, checked: Boolean): CheckBox
	{
		val checkBox = CheckBox(this)
		checkBox.text = text
		checkBox.isChecked = checked
		checkBox.textSize = 15f
		checkBox.setTextColor(palette.textPrimary)
		checkBox.buttonTintList = ColorStateList.valueOf(palette.accent)
		checkBox.setPadding(0, dp(7), 0, dp(7))
		parent.addView(checkBox, matchWrap())
		return checkBox
	}

	private fun addCheckBox(text: String, checked: Boolean): CheckBox
	{
		return addStyledCheckBox(contentLayout, text, checked)
	}

	private fun createActionButton(text: String, style: ButtonStyle, requiresConnection: Boolean, action: () -> Unit): Button
	{
		val button = Button(this)
		button.text = text
		button.textSize = 15f
		button.typeface = Typeface.DEFAULT_BOLD
		button.isAllCaps = false
		button.minHeight = 0
		button.minimumHeight = 0
		button.setPadding(dp(14), 0, dp(14), 0)

		val backgroundColor: Int
		val borderColor: Int
		val textColor: Int

		when (style)
		{
			ButtonStyle.Primary -> { backgroundColor = palette.accent; borderColor = palette.accent; textColor = palette.white }
			ButtonStyle.Secondary -> { backgroundColor = palette.surfaceAlt; borderColor = palette.border; textColor = palette.textPrimary }
			ButtonStyle.Success -> { backgroundColor = palette.success; borderColor = palette.success; textColor = palette.white }
			ButtonStyle.Danger -> { backgroundColor = palette.danger; borderColor = palette.danger; textColor = palette.white }
			ButtonStyle.Outline -> { backgroundColor = Color.TRANSPARENT; borderColor = palette.accent; textColor = palette.accent }
		}

		button.setTextColor(textColor)
		button.background = rippleDrawable(backgroundColor, borderColor, 15)
		button.setOnClickListener { action() }
		if (requiresConnection) commandButtons.add(button)
		return button
	}

	private fun addButton(text: String, action: () -> Unit): Button
	{
		val button = createActionButton(text, ButtonStyle.Secondary, true, action)
		contentLayout.addView(button, matchWrapWithTopMargin(8))
		return button
	}

	private fun createButtonRow(): LinearLayout
	{
		val row = LinearLayout(this)
		row.orientation = LinearLayout.HORIZONTAL
		row.gravity = Gravity.CENTER_VERTICAL
		return row
	}

	private fun weightedButtonParams(withLeftMargin: Boolean): LinearLayout.LayoutParams
	{
		val params = LinearLayout.LayoutParams(0, dp(52), 1f)
		if (withLeftMargin) params.setMargins(dp(8), 0, 0, 0)
		return params
	}

	private fun matchWrap(): LinearLayout.LayoutParams
	{
		return LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT)
	}

	private fun matchWrapWithTopMargin(topMargin: Int): LinearLayout.LayoutParams
	{
		val params = matchWrap()
		params.height = dp(52)
		params.setMargins(0, dp(topMargin), 0, 0)
		return params
	}

	private fun frameMatch(): FrameLayout.LayoutParams
	{
		return FrameLayout.LayoutParams(FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
	}

	private fun drawerFrameParams(): FrameLayout.LayoutParams
	{
		val width = minOf(dp(320), (resources.displayMetrics.widthPixels * 0.88f).toInt())
		val params = FrameLayout.LayoutParams(width, FrameLayout.LayoutParams.MATCH_PARENT)
		params.gravity = Gravity.END
		return params
	}

	private fun roundedDrawable(color: Int, radiusDp: Int, strokeColor: Int? = null, strokeWidthDp: Int = 0): GradientDrawable
	{
		return GradientDrawable().apply {
			shape = GradientDrawable.RECTANGLE
			setColor(color)
			cornerRadius = dp(radiusDp).toFloat()
			if (strokeColor != null && strokeWidthDp > 0) setStroke(dp(strokeWidthDp), strokeColor)
		}
	}

	private fun rippleDrawable(color: Int, strokeColor: Int, radiusDp: Int): RippleDrawable
	{
		val content = roundedDrawable(color, radiusDp, strokeColor, 1)
		val rippleColor = ColorStateList.valueOf(if (isDarkTheme) Color.argb(70, 255, 255, 255) else Color.argb(45, 0, 0, 0))
		return RippleDrawable(rippleColor, content, null)
	}

	private fun loadThemeMode(): ThemeMode
	{
		val value = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
			.getString(ThemePreferenceKey, ThemeMode.System.preferenceValue)
		return ThemeMode.entries.firstOrNull { it.preferenceValue == value } ?: ThemeMode.System
	}

	private fun saveThemeMode(mode: ThemeMode)
	{
		getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
			.edit()
			.putString(ThemePreferenceKey, mode.preferenceValue)
			.apply()
	}

	private fun applyThemeMode(mode: ThemeMode)
	{
		saveThemeMode(mode)
		themeMode = mode
		isDarkTheme = resolveDarkTheme(mode)
		setTheme(if (isDarkTheme) R.style.AppThemeDark else R.style.AppThemeLight)
		palette = createPalette(isDarkTheme)
		buildBaseUi()
		renderCurrentScreen()

		val modeName = when (mode)
		{
			ThemeMode.System -> tr("Sistema", "System")
			ThemeMode.Light -> tr("Chiaro", "Light")
			ThemeMode.Dark -> tr("Scuro", "Dark")
		}
		showStatus(tr("Tema applicato: $modeName", "Theme applied: $modeName"))
	}

	private fun loadAppLanguage(): AppLanguage
	{
		val value = getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
			.getString(LanguagePreferenceKey, AppLanguage.Italian.preferenceValue)
		return AppLanguage.entries.firstOrNull { it.preferenceValue == value } ?: AppLanguage.Italian
	}

	private fun saveAppLanguage(language: AppLanguage)
	{
		getSharedPreferences(PreferencesName, Context.MODE_PRIVATE)
			.edit()
			.putString(LanguagePreferenceKey, language.preferenceValue)
			.apply()
	}

	private fun applyAppLanguage(language: AppLanguage)
	{
		saveAppLanguage(language)
		appLanguage = language
		buildBaseUi()
		renderCurrentScreen()
		showStatus(tr("Lingua applicata: Italiano", "Language applied: English"))
	}

	private fun tr(italian: String, english: String): String
	{
		return if (appLanguage == AppLanguage.English) english else italian
	}

	private fun resolveDarkTheme(mode: ThemeMode): Boolean
	{
		return when (mode)
		{
			ThemeMode.Light -> false
			ThemeMode.Dark -> true
			ThemeMode.System -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
		}
	}

	private fun createPalette(dark: Boolean): AppPalette
	{
		return if (dark)
		{
			AppPalette(
				background = Color.rgb(15, 17, 21),
				surface = Color.rgb(26, 29, 35),
				surfaceAlt = Color.rgb(35, 39, 47),
				textPrimary = Color.rgb(245, 246, 248),
				textSecondary = Color.rgb(168, 176, 188),
				accent = Color.rgb(255, 103, 55),
				accentSoft = Color.rgb(64, 38, 30),
				border = Color.rgb(55, 61, 72),
				success = Color.rgb(42, 154, 92),
				danger = Color.rgb(210, 69, 69),
				warning = Color.rgb(220, 158, 55),
				white = Color.WHITE,
				scrim = Color.argb(150, 0, 0, 0)
			)
		}
		else
		{
			AppPalette(
				background = Color.rgb(245, 247, 250),
				surface = Color.WHITE,
				surfaceAlt = Color.rgb(238, 241, 245),
				textPrimary = Color.rgb(24, 27, 32),
				textSecondary = Color.rgb(98, 106, 118),
				accent = Color.rgb(225, 83, 38),
				accentSoft = Color.rgb(255, 233, 224),
				border = Color.rgb(218, 223, 230),
				success = Color.rgb(33, 145, 84),
				danger = Color.rgb(200, 57, 57),
				warning = Color.rgb(196, 130, 28),
				white = Color.WHITE,
				scrim = Color.argb(110, 0, 0, 0)
			)
		}
	}

	private fun dp(value: Int): Int
	{
		return (value * resources.displayMetrics.density).toInt()
	}

	private fun requestNeededPermissions()
	{
		val permissions = mutableListOf<String>()

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
		{
			if (checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED)
			{
				permissions.add(Manifest.permission.BLUETOOTH_SCAN)
			}

			if (checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
			{
				permissions.add(Manifest.permission.BLUETOOTH_CONNECT)
			}
		}
		else
		{
			if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
			{
				permissions.add(Manifest.permission.ACCESS_FINE_LOCATION)
			}
		}

		if (permissions.isNotEmpty())
		{
			requestPermissions(permissions.toTypedArray(), 1)
		}
	}

	private fun hasScanPermission(): Boolean
	{
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
		{
			checkSelfPermission(Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
		}
		else
		{
			checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
		}
	}

	private fun hasConnectPermission(): Boolean
	{
		return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
		{
			checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
		}
		else
		{
			true
		}
	}

	@SuppressLint("MissingPermission")
	private fun startScan()
	{
		if (!hasScanPermission())
		{
			requestNeededPermissions()
			showStatus(tr("Permessi Bluetooth mancanti.", "Bluetooth permissions are missing."))
			return
		}

		val adapter = bluetoothAdapter

		if (adapter == null || !adapter.isEnabled)
		{
			showStatus(tr("Bluetooth non attivo.", "Bluetooth is disabled."))
			return
		}

		if (isScanning)
		{
			stopScan()
			return
		}

		foundDevices.clear()
		deviceListLayout?.removeAllViews()
		showStatus(tr("Scansione in corso...", "Scanning..."))

		val filter = ScanFilter.Builder()
			.setServiceUuid(ParcelUuid(ServiceUuid))
			.build()

		val settings = ScanSettings.Builder()
			.setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
			.build()

		adapter.bluetoothLeScanner?.startScan(listOf(filter), settings, scanCallback)
		isScanning = true
		updateVisibleButtons()

		handler.postDelayed({ stopScan() }, 12000)
	}

	@SuppressLint("MissingPermission")
	private fun stopScan()
	{
		if (!isScanning)
		{
			return
		}

		if (hasScanPermission())
		{
			bluetoothAdapter?.bluetoothLeScanner?.stopScan(scanCallback)
		}

		isScanning = false
		showStatus(tr("Scansione terminata.", "Scan completed."))
		updateVisibleButtons()
	}

	@SuppressLint("MissingPermission")
	private fun addDeviceButton(name: String, address: String, device: BluetoothDevice)
	{
		val layout = deviceListLayout ?: return
		val button = createActionButton("$name\n$address", ButtonStyle.Secondary, false) {
			stopScan()
			selectedDeviceName = name
			updateSelectedDeviceLabel()
			shouldOpenPinDialogAfterConnection = true
			pinDialogAlreadyShown = false
			connectToDevice(device)
		}
		button.gravity = Gravity.CENTER_VERTICAL or Gravity.START
		button.setPadding(dp(16), dp(8), dp(16), dp(8))
		val params = matchWrapWithTopMargin(8)
		params.height = dp(68)
		layout.addView(button, params)
	}

	@SuppressLint("MissingPermission")
	private fun connectToDevice(device: BluetoothDevice)
	{
		if (!hasConnectPermission())
		{
			requestNeededPermissions()
			showStatus(tr("Permesso connessione Bluetooth mancante.", "Bluetooth connection permission is missing."))
			return
		}

		showStatus(tr("Connessione a ${device.address}...", "Connecting to ${device.address}..."))
		bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
		updateVisibleButtons()
	}

	private fun showStartupDialog()
	{
		AlertDialog.Builder(this)
			.setTitle("Open Airsoft Countdown")
			.setMessage(
				"Lorem ipsum dolor sit amet, consectetur adipiscing elit. " +
				"Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua."
			)
			.setPositiveButton(tr("Continua", "Continue"), null)
			.setOnDismissListener { requestNeededPermissions() }
			.show()
	}

	private fun showConfigRestartDialog()
	{
		AlertDialog.Builder(this)
			.setTitle(tr("Riavvio necessario", "Restart required"))
			.setMessage(tr("Riavvia il dispositivo Open Airsoft Countdown", "Restart the Open Airsoft Countdown device"))
			.setPositiveButton("OK", null)
			.show()
	}

	private fun showPinDialog()
	{
		if (commandCharacteristic == null)
		{
			showStatus(tr("Dispositivo non pronto.", "Device not ready."))
			return
		}

		val fieldContainer = FrameLayout(this)
		fieldContainer.background = roundedDrawable(palette.surfaceAlt, 15, palette.border, 1)

		val input = EditText(this)
		input.hint = tr("PIN admin", "Admin PIN")
		input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
		input.transformationMethod = AsteriskPasswordTransformationMethod()
		input.setSingleLine(true)
		input.setTextColor(palette.textPrimary)
		input.setHintTextColor(palette.textSecondary)
		input.textSize = 16f
		input.setPadding(dp(14), 0, dp(54), 0)
		input.background = null

		val inputParams = FrameLayout.LayoutParams(
			FrameLayout.LayoutParams.MATCH_PARENT,
			dp(54)
		)
		fieldContainer.addView(input, inputParams)

		val visibilityButton = ImageButton(this)
		visibilityButton.setImageResource(android.R.drawable.ic_menu_view)
		visibilityButton.imageTintList = ColorStateList.valueOf(palette.textSecondary)
		visibilityButton.contentDescription = tr("Mostra o nascondi PIN", "Show or hide PIN")
		visibilityButton.background = null
		visibilityButton.setPadding(dp(10), dp(10), dp(10), dp(10))

		val visibilityParams = FrameLayout.LayoutParams(dp(48), dp(48))
		visibilityParams.gravity = Gravity.END or Gravity.CENTER_VERTICAL
		visibilityParams.setMargins(0, 0, dp(3), 0)
		fieldContainer.addView(visibilityButton, visibilityParams)

		var pinVisible = false
		input.transformationMethod = AsteriskPasswordTransformationMethod()
		visibilityButton.alpha = 0.65f
		visibilityButton.setOnClickListener {
			pinVisible = !pinVisible
			input.transformationMethod = if (pinVisible)
			{
				HideReturnsTransformationMethod.getInstance()
			}
			else
			{
				AsteriskPasswordTransformationMethod()
			}
			input.setSelection(input.text.length)
			visibilityButton.alpha = if (pinVisible) 1f else 0.65f
		}

		val dialogContent = LinearLayout(this)
		dialogContent.orientation = LinearLayout.VERTICAL
		dialogContent.setPadding(dp(24), dp(8), dp(24), 0)
		dialogContent.addView(
			fieldContainer,
			LinearLayout.LayoutParams(
				LinearLayout.LayoutParams.MATCH_PARENT,
				LinearLayout.LayoutParams.WRAP_CONTENT
			)
		)

		val dialog = AlertDialog.Builder(this)
			.setTitle(tr("PIN amministratore", "Administrator PIN"))
			.setMessage(tr("Inserisci il PIN per accedere al dispositivo.", "Enter the PIN to access the device."))
			.setView(dialogContent)
			.setPositiveButton("Login") { _, _ ->
				sendCommand("LOGIN:${input.text}")
			}
			.setNegativeButton(tr("Annulla", "Cancel"), null)
			.create()

		dialog.setOnShowListener {
			pinVisible = false
			input.transformationMethod = AsteriskPasswordTransformationMethod()
			input.setSelection(input.text.length)
			visibilityButton.alpha = 0.65f
			input.requestFocus()
			dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE)

			input.postDelayed({
				val inputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
				inputMethodManager.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
			}, 200)
		}

		dialog.show()
	}

	@SuppressLint("MissingPermission")
	private fun discoverServicesIfConnected()
	{
		val gatt = bluetoothGatt ?: return

		if (!isConnected || !hasConnectPermission())
		{
			return
		}

		gatt.discoverServices()
	}

	@SuppressLint("MissingPermission")
	private fun enableNotifications(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic)
	{
		if (!hasConnectPermission())
		{
			return
		}

		gatt.setCharacteristicNotification(characteristic, true)

		val descriptor = characteristic.getDescriptor(ClientConfigurationDescriptorUuid) ?: return

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
		{
			gatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
		}
		else
		{
			descriptor.value = BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE
			gatt.writeDescriptor(descriptor)
		}
	}

	@SuppressLint("MissingPermission")
	private fun sendCommand(command: String)
	{
		val gatt = bluetoothGatt
		val characteristic = commandCharacteristic

		if (gatt == null || characteristic == null || !hasConnectPermission())
		{
			showStatus(tr("Dispositivo non pronto.", "Device not ready."))
			return
		}

		val bytes = command.toByteArray(Charset.forName("UTF-8"))

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
		{
			gatt.writeCharacteristic(characteristic, bytes, BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT)
		}
		else
		{
			characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
			characteristic.value = bytes
			gatt.writeCharacteristic(characteristic)
		}

		showStatus(tr("Comando inviato: $command", "Command sent: $command"))
	}

	@SuppressLint("MissingPermission")
	private fun disconnect()
	{
		if (hasConnectPermission())
		{
			bluetoothGatt?.disconnect()
			bluetoothGatt?.close()
		}

		bluetoothGatt = null
		commandCharacteristic = null
		responseCharacteristic = null
		isConnected = false
		shouldOpenPinDialogAfterConnection = false
		pinDialogAlreadyShown = false
		selectedDeviceName = "--"
		loadUsersAfterConfig = false
		clearCachedStatus()
		updateSelectedDeviceLabel()

		if (!isFinishing && currentScreen == Screen.Device)
		{
			renderCurrentScreen()
		}
		else
		{
			updateVisibleButtons()
		}
	}

	private fun handleBleResponse(response: String)
	{
		runOnUiThreadSafe {
			rawStatusLabel?.text = response

			when
			{
				response.startsWith("STATUS:") -> parseStatus(response)
				response.startsWith("CONFIG:") -> parseConfig(response)
				response.startsWith("USERS_BEGIN:") -> beginUsersResponse(response)
				response.startsWith("USER:") -> parseUser(response)
				response == "USERS_END" -> finishUsersResponse()
				response.startsWith("ERR:") ->
				{
					isReceivingUsers = false
					pendingConfigRestartDialog = false
					loadUsersAfterConfig = false
					showStatus(response)
					Toast.makeText(this, response, Toast.LENGTH_SHORT).show()
				}
				response.startsWith("OK:") ->
				{
					showStatus(response)
					Toast.makeText(this, response, Toast.LENGTH_SHORT).show()

					if (response.startsWith("OK:LOGIN"))
					{
						loadUsersAfterConfig = true
						showStatus(tr("Login eseguito. Lettura configurazione...", "Login successful. Reading configuration..."))
						handler.postDelayed({ sendCommand("GETCONFIG") }, 150)
					}

					if (response.startsWith("OK:CONFIG_SAVED") && pendingConfigRestartDialog)
					{
						pendingConfigRestartDialog = false
						showConfigRestartDialog()
					}
				}
			}
		}
	}

	private fun parseConfig(response: String)
	{
		val values = parseProtocolValues(response.removePrefix("CONFIG:"))

		deviceConfig.adminPin = values["adminPin"] ?: deviceConfig.adminPin
		deviceConfig.bleName = values["bleName"] ?: deviceConfig.bleName
		values["language"]?.let {
			deviceConfig.language = if (it == "en") "en" else "it"
		}
		deviceConfig.authorizedUserIds =
			values["authorizedUserIds"] ?: deviceConfig.authorizedUserIds
		values["soundEnabled"]?.let { deviceConfig.soundEnabled = it == "1" }
		values["rfid"]?.let { deviceConfig.rfid = it == "1" }
		values["fingerprint"]?.let { deviceConfig.fingerprint = it == "1" }
		deviceConfig.maxErrorCount = values["maxErrorCount"] ?: deviceConfig.maxErrorCount
		deviceConfig.errorCountdownSeconds = values["errorCountdownSeconds"] ?: deviceConfig.errorCountdownSeconds

		updateConfigViews()

		if (loadUsersAfterConfig)
		{
			loadUsersAfterConfig = false
			showStatus(tr("Configurazione letta. Lettura utenti...", "Configuration loaded. Reading users..."))
			handler.postDelayed({ sendCommand("GETUSERS") }, 150)
		}
		else
		{
			showStatus(tr("Configurazione letta dal dispositivo.", "Configuration read from device."))
		}
	}

	private fun updateConfigViews()
	{
		configAdminPinInput?.setText(deviceConfig.adminPin)
		configBleNameInput?.setText(deviceConfig.bleName)

		if (deviceConfig.language == "en")
		{
			configLanguageEnglishButton?.isChecked = true
		}
		else
		{
			configLanguageItalianButton?.isChecked = true
		}

		configAuthorizedUserIdsInput?.setText(deviceConfig.authorizedUserIds)
		configSoundCheck?.isChecked = deviceConfig.soundEnabled
		configRfidCheck?.isChecked = deviceConfig.rfid
		configFingerprintCheck?.isChecked = deviceConfig.fingerprint
		configMaxErrorInput?.setText(deviceConfig.maxErrorCount)
		configErrorCountdownInput?.setText(formatSecondsAsHhmmss(deviceConfig.errorCountdownSeconds))
	}

	private fun beginUsersResponse(response: String)
	{
		val countText = response.substringAfter("count=", "")
		expectedUserCount = countText.toIntOrNull()
		deviceUsers.clear()
		isReceivingUsers = true
		updateUsersViews()
	}

	private fun parseUser(response: String)
	{
		if (!isReceivingUsers)
		{
			deviceUsers.clear()
			isReceivingUsers = true
		}

		val values = parseProtocolValues(response.removePrefix("USER:"))
		val id = values["id"] ?: return
		val name = values["name"] ?: return
		val uid = values["uid"] ?: return
		val pin = values["pin"] ?: return

		deviceUsers.removeAll { it.id == id }
		deviceUsers.add(DeviceUser(id, name, uid, pin))
		deviceUsers.sortBy { it.id.toIntOrNull() ?: Int.MAX_VALUE }
		updateUsersViews()
	}

	private fun finishUsersResponse()
	{
		isReceivingUsers = false
		updateUsersViews()

		val expected = expectedUserCount
		showStatus(
			if (expected != null && expected != deviceUsers.size)
			{
				tr("Utenti ricevuti: ${deviceUsers.size} di $expected.", "Users received: ${deviceUsers.size} of $expected.")
			}
			else
			{
				tr("Utenti letti dal dispositivo: ${deviceUsers.size}.", "Users read from device: ${deviceUsers.size}.")
			}
		)
	}

	private fun updateUsersViews()
	{
		val summary = usersSummaryLabel ?: return
		val layout = usersListLayout ?: return

		summary.text = when
		{
			isReceivingUsers -> tr("Ricezione utenti... (${deviceUsers.size})", "Receiving users... (${deviceUsers.size})")
			deviceUsers.isEmpty() -> tr("Nessun utente caricato.", "No users loaded.")
			else -> tr("Utenti presenti: ${deviceUsers.size}", "Users available: ${deviceUsers.size}")
		}

		layout.removeAllViews()

		for (user in deviceUsers)
		{
			val row = LinearLayout(this)
			row.orientation = LinearLayout.HORIZONTAL
			row.gravity = Gravity.CENTER_VERTICAL
			row.setPadding(dp(14), dp(12), dp(10), dp(12))
			row.background = roundedDrawable(palette.surfaceAlt, 15, palette.border, 1)

			val userText = TextView(this)
			userText.text = tr("ID: ${user.id}\nNome: ${user.name}\nUID: ${user.uid}\nPIN: ${user.pin}", "ID: ${user.id}\nName: ${user.name}\nUID: ${user.uid}\nPIN: ${user.pin}")
			userText.textSize = 15f
			userText.setTextColor(palette.textPrimary)
			userText.setLineSpacing(0f, 1.08f)
			row.addView(userText, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))

			val actions = LinearLayout(this)
			actions.orientation = LinearLayout.VERTICAL
			actions.gravity = Gravity.CENTER

			val editButton = createUserIconButton(
				R.drawable.ic_edit,
				tr("Modifica ${user.name}", "Edit ${user.name}"),
				palette.accent
			) {
				showEditUserDialog(user)
			}

			val deleteButton = createUserIconButton(
				R.drawable.ic_delete,
				tr("Elimina ${user.name}", "Delete ${user.name}"),
				palette.danger
			) {
				showDeleteUserDialog(user)
			}

			actions.addView(editButton, LinearLayout.LayoutParams(dp(44), dp(44)))
			val deleteParams = LinearLayout.LayoutParams(dp(44), dp(44))
			deleteParams.setMargins(0, dp(6), 0, 0)
			actions.addView(deleteButton, deleteParams)
			row.addView(actions, LinearLayout.LayoutParams(dp(52), LinearLayout.LayoutParams.WRAP_CONTENT))

			val params = matchWrap()
			params.setMargins(0, 0, 0, dp(10))
			layout.addView(row, params)
		}
	}

	private fun createUserIconButton(drawableId: Int, description: String, tintColor: Int, action: () -> Unit): ImageButton
	{
		val button = ImageButton(this)
		button.setImageResource(drawableId)
		button.imageTintList = ColorStateList.valueOf(tintColor)
		button.contentDescription = description
		button.setPadding(dp(10), dp(10), dp(10), dp(10))
		button.background = rippleDrawable(Color.TRANSPARENT, palette.border, 13)
		button.setOnClickListener { action() }
		return button
	}

	private fun showEditUserDialog(user: DeviceUser)
	{
		val dialogContent = LinearLayout(this)
		dialogContent.orientation = LinearLayout.VERTICAL
		dialogContent.setPadding(dp(24), dp(4), dp(24), dp(4))

		addFieldLabel(dialogContent, tr("Nome utente", "User name"))
		val nameInput = addStyledEditText(dialogContent, tr("Nome utente", "User name"), InputType.TYPE_CLASS_TEXT)
		nameInput.setText(user.name)

		addFieldLabel(dialogContent, "UID NFC")
		val uidInput = addStyledEditText(dialogContent, "UID NFC", InputType.TYPE_CLASS_TEXT)
		uidInput.setText(user.uid)

		addFieldLabel(dialogContent, tr("PIN utente (6 cifre)", "User PIN (6 digits)"))
		val pinInput = addStyledEditText(dialogContent, tr("PIN utente", "User PIN"), InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD)
		configureSixDigitPinInput(pinInput)
		pinInput.setText(user.pin)
		pinInput.setSelection(pinInput.text.length)

		val dialog = AlertDialog.Builder(this)
			.setTitle(tr("Modifica utente", "Edit user"))
			.setView(dialogContent)
			.setPositiveButton(tr("Salva", "Save"), null)
			.setNegativeButton(tr("Annulla", "Cancel"), null)
			.create()

		dialog.setOnShowListener {
			dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
				val name = nameInput.text.toString().trim()
				val uid = uidInput.text.toString().trim().uppercase()
				val pin = pinInput.text.toString().trim()

				when
				{
					!isValidUserName(name) -> showStatus(tr("Nome utente non valido.", "Invalid user name."))
					!isValidUserUid(uid) -> showStatus(tr("UID NFC non valido.", "Invalid NFC UID."))
					!isValidSixDigitPin(pin) -> showStatus(tr("Il PIN utente deve contenere esattamente 6 cifre.", "The user PIN must contain exactly 6 digits."))
					else ->
					{
						showStatus(tr("Modifica utente...", "Updating user..."))
						sendCommand("UPDATEUSER:id=${user.id};name=$name;uid=$uid;pin=$pin")
						dialog.dismiss()
					}
				}
			}
		}

		dialog.show()
	}

	private fun showDeleteUserDialog(user: DeviceUser)
	{
		AlertDialog.Builder(this)
			.setTitle(tr("Elimina utente", "Delete user"))
			.setMessage(tr("Vuoi eliminare ${user.name} (ID ${user.id})?", "Do you want to delete ${user.name} (ID ${user.id})?"))
			.setPositiveButton(tr("Elimina", "Delete")) { _, _ ->
				showStatus(tr("Eliminazione utente...", "Deleting user..."))
				sendCommand("DELUSER:${user.id}")
			}
			.setNegativeButton(tr("Annulla", "Cancel"), null)
			.show()
	}

	private fun configureSixDigitPinInput(input: EditText)
	{
		input.filters = arrayOf(InputFilter.LengthFilter(6))
		input.inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
	}

	private fun isValidSixDigitPin(pin: String): Boolean
	{
		return pin.length == 6 && pin.all { it.isDigit() }
	}

	private fun validateAuthorizedUserIds(value: String): String?
	{
		if (value.isEmpty())
		{
			return tr(
				"Inserisci almeno un ID utente.",
				"Enter at least one user ID."
			)
		}

		if (value.any { it.isWhitespace() })
		{
			return tr(
				"Gli ID utenti non possono contenere spazi.",
				"User IDs cannot contain spaces."
			)
		}

		if (value.any { !it.isDigit() && it != ',' && it != ';' })
		{
			return tr(
				"Sono ammessi soltanto numeri, virgole e punti e virgola.",
				"Only numbers, commas, and semicolons are allowed."
			)
		}

		val hasComma = value.contains(',')
		val hasSemicolon = value.contains(';')

		if (hasComma && hasSemicolon)
		{
			return tr(
				"Non puoi usare virgole e punti e virgola nella stessa configurazione.",
				"Commas and semicolons cannot be used in the same configuration."
			)
		}

		if (value.count { it == ',' } > 3)
		{
			return tr(
				"Con la virgola puoi indicare al massimo 4 utenti.",
				"When using commas, you can specify no more than 4 users."
			)
		}

		val separator =
			when
			{
				hasComma -> ','
				hasSemicolon -> ';'
				else -> null
			}

		val ids =
			if (separator == null)
			{
				listOf(value)
			}
			else
			{
				value.split(separator)
			}

		if (ids.any { it.isEmpty() })
		{
			return tr(
				"Formato ID non valido: non lasciare valori vuoti tra i separatori.",
				"Invalid ID format: do not leave empty values between separators."
			)
		}

		if (ids.any { id -> id.toLongOrNull() == null || id.toLong() <= 0L })
		{
			return tr(
				"Ogni ID deve essere un numero intero maggiore di zero.",
				"Each ID must be an integer greater than zero."
			)
		}

		if (ids.distinct().size != ids.size)
		{
			return tr(
				"Lo stesso ID non può essere inserito più di una volta.",
				"The same ID cannot be entered more than once."
			)
		}

		return null
	}

	private fun isValidUserName(name: String): Boolean
	{
		return name.isNotEmpty() && name.length <= 32 && !name.contains(';') && !name.contains('=')
	}

	private fun isValidUserUid(uid: String): Boolean
	{
		return uid.isNotEmpty() && uid.length <= 32 && uid.all { it.isDigit() || it in 'A'..'F' }
	}

	private fun parseProtocolValues(body: String): Map<String, String>
	{
		val values = mutableMapOf<String, String>()

		for (part in body.split(';'))
		{
			val index = part.indexOf('=')

			if (index > 0)
			{
				values[part.substring(0, index)] = part.substring(index + 1)
			}
		}

		return values
	}

	private fun clearCachedStatus()
	{
		hasCachedStatus = false
		cachedRemainingSeconds = 0L
		cachedMode = "--"
		cachedErrors = "--"
		cachedLocked = false
		cachedLogged = false
	}

	private fun parseStatus(response: String)
	{
		val body = response.removePrefix("STATUS:")
		val parts = body.split(";")
		val mode = parts.firstOrNull() ?: "--"
		val values = mutableMapOf<String, String>()

		for (part in parts.drop(1))
		{
			val index = part.indexOf('=')

			if (index > 0)
			{
				values[part.substring(0, index)] = part.substring(index + 1)
			}
		}

		val remaining = values["remaining"]?.toUIntOrNull()?.toLong() ?: 0L
		val errors = values["errors"] ?: "--"
		val locked = values["locked"] == "1"
		val logged = values["logged"] == "1"

		hasCachedStatus = true
		cachedRemainingSeconds = remaining
		cachedMode = mode
		cachedErrors = errors
		cachedLocked = locked
		cachedLogged = logged

		applyCachedStatusToMainScreen()
	}

	private fun applyCachedStatusToMainScreen()
	{
		if (!hasCachedStatus)
		{
			return
		}

		timerLabel?.text = formatSeconds(cachedRemainingSeconds)
		modeLabel?.text = tr(
			"Stato: $cachedMode  |  Login: ${if (cachedLogged) "OK" else "NO"}",
			"Status: $cachedMode  |  Login: ${if (cachedLogged) "OK" else "NO"}"
		)
		errorsLabel?.text = tr(
			"Errori: $cachedErrors  |  Blocco: ${if (cachedLocked) "SI" else "NO"}",
			"Errors: $cachedErrors  |  Locked: ${if (cachedLocked) "YES" else "NO"}"
		)
	}

	private fun formatSecondsAsHhmmss(secondsText: String): String
	{
		val totalSeconds = secondsText.toIntOrNull()?.coerceIn(0, 3600) ?: 0
		val hours = totalSeconds / 3600
		val minutes = (totalSeconds % 3600) / 60
		val seconds = totalSeconds % 60
		return "%02d%02d%02d".format(hours, minutes, seconds)
	}

	private fun parseHhmmssToSeconds(value: String): Int?
	{
		if (value.length != 6 || value.any { !it.isDigit() })
		{
			return null
		}

		val hours = value.substring(0, 2).toInt()
		val minutes = value.substring(2, 4).toInt()
		val seconds = value.substring(4, 6).toInt()

		if (minutes > 59 || seconds > 59)
		{
			return null
		}

		return hours * 3600 + minutes * 60 + seconds
	}

	private fun formatSeconds(seconds: Long): String
	{
		val hours = seconds / 3600
		val minutes = (seconds % 3600) / 60
		val remainingSeconds = seconds % 60

		return if (hours > 0)
		{
			"%02d:%02d:%02d".format(hours, minutes, remainingSeconds)
		}
		else
		{
			"%02d:%02d".format(minutes, remainingSeconds)
		}
	}

	@SuppressLint("MissingPermission")
	private fun getDeviceName(device: BluetoothDevice, result: ScanResult): String?
	{
		return result.scanRecord?.deviceName ?: if (hasConnectPermission()) device.name else null
	}

	private fun showStatus(message: String)
	{
		statusLabel.text = message
	}

	private fun updateSelectedDeviceLabel()
	{
		selectedDeviceLabel?.text = tr("Dispositivo selezionato\n$selectedDeviceName", "Selected device\n$selectedDeviceName")
	}

	private fun updateVisibleButtons()
	{
		scanButton?.text = if (isScanning) tr("Ferma scansione", "Stop scan") else tr("Scansiona dispositivo", "Scan for device")
		disconnectButton?.isEnabled = isConnected

		val ready = isConnected && commandCharacteristic != null
		for (button in commandButtons)
		{
			button.isEnabled = ready
			button.alpha = if (ready) 1f else 0.45f
		}
	}

	private fun runOnUiThreadSafe(action: () -> Unit)
	{
		if (Looper.myLooper() == Looper.getMainLooper())
		{
			action()
		}
		else
		{
			runOnUiThread { action() }
		}
	}
}
