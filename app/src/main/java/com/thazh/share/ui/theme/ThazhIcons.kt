package com.thazh.share.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
import androidx.compose.ui.unit.dp
import io.github.neoruaa.glasmio.icons.GlasmioIconResolver
import io.github.neoruaa.glasmio.icons.SfSymbol
import io.github.neoruaa.glasmio.icons.SfSymbolCatalog

/**
 * Purpose-accurate SF Symbol system names and Glasmio [SfSymbol] constants for Thazh Share.
 * Every symbol maps to an authentic, purpose-designed Apple SF Symbol vector glyph in [ThazhIconResolver]
 * so that every tab, radar node, protocol pill, file category, and settings row has an unmistakable icon.
 */
object ThazhSymbols {
    // Primary bottom navigation tab items (using Glasmio's SfSymbol enum, overridden by ThazhIconResolver)
    // TabSend = Paperplane / AirDrop Send; TabReceive = Tray Arrow Down; TabLinkShare = Cloud Link; TabReceivedFiles = Folder Stack
    val TabSend: SfSymbol = SfSymbol.RadioWaves
    val TabReceive: SfSymbol = SfSymbol.DownloadFilled
    val TabLinkShare: SfSymbol = SfSymbol.CloudFilled
    val TabReceivedFiles: SfSymbol = SfSymbol.Layers

    // Core P2P & Transfer symbols
    const val SendPaperPlane: String = "paperplane.fill"
    val WifiTethering: String = SfSymbol.RadioWaves.systemName
    const val ArrowUp: String = "arrow.up"
    const val ArrowDown: String = "arrow.down"
    const val FolderOpen: String = "folder.fill"
    val Settings: String = SfSymbol.Settings.systemName
    val Person: String = SfSymbol.PersonFilled.systemName
    const val AccountCircle: String = "person.crop.circle"
    const val Logout: String = "rectangle.portrait.and.arrow.right"
    const val Bluetooth: String = "dot.radiowaves.left.and.right.ble"
    const val Nfc: String = "sensor.tag.radiowaves.forward"
    const val BatterySaver: String = "battery.100.bolt"
    const val ReduceTransparency: String = "square.on.square.dashed"
    val LiquidGlass: String = SfSymbol.Sparkles.systemName
    const val ThemeSystem: String = "circle.lefthalf.filled"
    const val ThemeLight: String = "sun.max.fill"
    const val ThemeDark: String = "moon.fill"
    const val Devices: String = "laptopcomputer.and.iphone"
    val History: String = SfSymbol.Clock.systemName
    val Radar: String = SfSymbol.Safari.systemName
    const val WifiOff: String = "wifi.slash"
    val MoreHoriz: String = SfSymbol.Ellipsis.systemName
    const val Edit: String = "pencil"
    const val Delete: String = "trash"
    const val Share: String = "square.and.arrow.up"
    val Download: String = SfSymbol.Download.systemName
    val Info: String = SfSymbol.InfoCircle.systemName
    const val OpenInNew: String = "arrow.up.right.square"
    val Close: String = SfSymbol.Close.systemName
    val Refresh: String = SfSymbol.ArrowClockwise.systemName
    const val Link: String = "link"
    const val LinkOff: String = "link.badge.minus"
    const val CloudUpload: String = "icloud.and.arrow.up"
    const val CloudDone: String = "checkmark.icloud"
    const val Copy: String = "doc.on.doc"
    const val QrCode: String = "qrcode"
    const val AttachFile: String = "paperclip"
    const val PhotoLibrary: String = "photo.on.rectangle"
    const val SwapVert: String = "arrow.up.arrow.down"
    const val Checklist: String = "checklist"
    const val Sort: String = "line.3.horizontal.decrease"
    const val SelectAll: String = "checkmark.circle.badge.questionmark"
    const val CheckCircle: String = "checkmark.circle.fill"
    const val RadioUnchecked: String = "circle"
    const val ChevronRight: String = "chevron.right"
    val Back: String = SfSymbol.ChevronBack.systemName
    val Search: String = SfSymbol.Search.systemName

    // Visibility, incoming dialog & extended settings symbols
    const val VisibilityOn: String = "eye"
    const val VisibilityOff: String = "eye.slash"
    val Timer: String = SfSymbol.ClockFilled.systemName
    const val AllInclusive: String = "infinity"
    const val ShieldCheck: String = "checkmark.shield"
    const val NotificationsActive: String = "bell.badge"
    const val OverlayPrompt: String = "pip.enter"
    const val SaveAlt: String = "square.and.arrow.down"
    const val Storage: String = "internaldrive"
    const val Cleaning: String = "paintbrush"
    const val DeleteSweep: String = "trash.slash"

    // File category symbols
    const val FileImage: String = "photo.fill"
    const val FileVideo: String = "film.fill"
    val FileAudio: String = SfSymbol.MusicNote.systemName
    const val FileDocument: String = "doc.text.fill"
    const val FileArchive: String = "archivebox.fill"
    const val FileApk: String = "shippingbox.fill"
    const val FileOther: String = "doc.fill"
    const val Globe: String = "globe"
}

/**
 * Complete, self-contained SF Symbol vector resolver for Thazh Share.
 * Resolves every SF Symbol name (verified against [SfSymbolCatalog]) to a purpose-designed
 * Apple SF-proportioned [ImageVector] — zero Material Icons fallbacks.
 */
object ThazhIconResolver : GlasmioIconResolver {
    override fun resolve(systemName: String): ImageVector {
        val canonical = SfSymbol.entries.firstOrNull {
            it.systemName == systemName ||
                (SfSymbolCatalog.codePointOrNull(systemName) != null &&
                    it.codePoint == SfSymbolCatalog.codePointOrNull(systemName))
        }?.systemName ?: systemName

        return when {
            // 1. Primary Navigation Tabs & Core Transfer Actions
            canonical == ThazhSymbols.SendPaperPlane -> SfVectorGlyphs.PaperPlaneSend
            canonical == SfSymbol.RadioWaves.systemName ||
                canonical == "antenna.radiowaves.left.and.right" ||
                canonical.startsWith("dot.radiowaves") && canonical != ThazhSymbols.Bluetooth ->
                SfVectorGlyphs.RadiowavesLeftRight

            canonical == SfSymbol.DownloadFilled.systemName ||
                canonical == ThazhSymbols.Download ||
                canonical == "arrow.down.circle" ||
                canonical == "arrow.down.circle.fill" ->
                SfVectorGlyphs.ArrowDownCircle

            canonical == ThazhSymbols.SaveAlt ||
                canonical == "tray.and.arrow.down" ||
                canonical == "tray.and.arrow.down.fill" ->
                SfVectorGlyphs.TrayArrowDown

            canonical == SfSymbol.CloudFilled.systemName ||
                canonical == SfSymbol.Cloud.systemName ||
                canonical == ThazhSymbols.CloudUpload ||
                canonical == "icloud.and.arrow.up" ->
                SfVectorGlyphs.CloudUpload

            canonical == ThazhSymbols.CloudDone ||
                canonical == "checkmark.icloud" ->
                SfVectorGlyphs.CloudCheckmark

            canonical == SfSymbol.Layers.systemName ||
                canonical == ThazhSymbols.FolderOpen ||
                canonical.startsWith("folder") ->
                SfVectorGlyphs.FolderFill

            // 2. Wireless Protocols (BLE, NFC, Radar, Wi-Fi Off)
            canonical == ThazhSymbols.Bluetooth -> SfVectorGlyphs.BluetoothWave
            canonical == ThazhSymbols.Nfc -> SfVectorGlyphs.NfcWaveTag
            canonical == ThazhSymbols.Radar || canonical.startsWith("safari") -> SfVectorGlyphs.RadarCompass
            canonical == ThazhSymbols.WifiOff || canonical == "wifi.slash" -> SfVectorGlyphs.WifiSlash

            // 3. Visibility, Security & Notifications
            canonical == ThazhSymbols.VisibilityOn || canonical == "eye" || canonical == "eye.fill" ->
                SfVectorGlyphs.EyeOpen
            canonical == ThazhSymbols.VisibilityOff || canonical.startsWith("eye.slash") ->
                SfVectorGlyphs.EyeSlash
            canonical == ThazhSymbols.Timer || canonical == SfSymbol.ClockFilled.systemName || canonical == "timer" ->
                SfVectorGlyphs.StopwatchTimer
            canonical == ThazhSymbols.History || canonical == SfSymbol.Clock.systemName || canonical == "clock" ->
                SfVectorGlyphs.ClockHistory
            canonical == ThazhSymbols.AllInclusive || canonical == "infinity" ->
                SfVectorGlyphs.InfinityLoop
            canonical == ThazhSymbols.ShieldCheck || canonical.startsWith("checkmark.shield") ->
                SfVectorGlyphs.ShieldCheck
            canonical == ThazhSymbols.NotificationsActive || canonical.startsWith("bell") ->
                SfVectorGlyphs.BellWave
            canonical == ThazhSymbols.OverlayPrompt || canonical == ThazhSymbols.ReduceTransparency ||
                canonical.startsWith("square.on.square") ->
                SfVectorGlyphs.SquareOnSquare

            // 4. Account, Devices, Links & QR
            canonical == ThazhSymbols.AccountCircle || canonical.startsWith("person.crop.circle") ->
                SfVectorGlyphs.PersonCropCircle
            canonical == ThazhSymbols.Person || canonical.startsWith("person") ->
                SfVectorGlyphs.PersonSilhouette
            canonical == ThazhSymbols.Devices ->
                SfVectorGlyphs.LaptopAndIphone
            canonical == ThazhSymbols.Logout || canonical.startsWith("rectangle.portrait.and.arrow") ->
                SfVectorGlyphs.PortraitArrowRight
            canonical == ThazhSymbols.Link ->
                SfVectorGlyphs.LinkChain
            canonical == ThazhSymbols.LinkOff ->
                SfVectorGlyphs.LinkOff
            canonical == ThazhSymbols.Copy || canonical.startsWith("doc.on.doc") ->
                SfVectorGlyphs.DocOnDoc
            canonical == ThazhSymbols.QrCode || canonical.startsWith("qrcode") ->
                SfVectorGlyphs.QrCodeViewfinder
            canonical == ThazhSymbols.OpenInNew || canonical == "arrow.up.right.square" ->
                SfVectorGlyphs.ArrowUpRightSquare
            canonical == ThazhSymbols.Share || canonical == "square.and.arrow.up" ->
                SfVectorGlyphs.ShareBoxArrowUp

            // 5. Appearance, Storage & Data Management
            canonical == ThazhSymbols.Settings || canonical.startsWith("gear") ->
                SfVectorGlyphs.Gearshape
            canonical == ThazhSymbols.LiquidGlass || canonical.startsWith("sparkles") ->
                SfVectorGlyphs.Sparkles
            canonical == ThazhSymbols.ThemeSystem || canonical == "circle.lefthalf.filled" ->
                SfVectorGlyphs.CircleLeftHalfFilled
            canonical == ThazhSymbols.ThemeLight || canonical.startsWith("sun") ->
                SfVectorGlyphs.SunMax
            canonical == ThazhSymbols.ThemeDark || canonical.startsWith("moon") ->
                SfVectorGlyphs.MoonCrescent
            canonical == ThazhSymbols.Storage || canonical == "internaldrive" ->
                SfVectorGlyphs.InternalDrive
            canonical == ThazhSymbols.Cleaning || canonical.startsWith("paintbrush") ->
                SfVectorGlyphs.SparkleBrush
            canonical == ThazhSymbols.DeleteSweep || canonical == "trash.slash" ->
                SfVectorGlyphs.TrashSweep
            canonical == ThazhSymbols.Delete || canonical.startsWith("trash") ->
                SfVectorGlyphs.TrashCan
            canonical == ThazhSymbols.BatterySaver || canonical.startsWith("battery") ->
                SfVectorGlyphs.BatteryBolt
            canonical == ThazhSymbols.Info || canonical.startsWith("info") ->
                SfVectorGlyphs.InfoCircle

            // 6. File Categories & Media Pickers
            canonical == ThazhSymbols.AttachFile || canonical == "paperclip" ->
                SfVectorGlyphs.Paperclip
            canonical == ThazhSymbols.PhotoLibrary || canonical == ThazhSymbols.FileImage ||
                canonical.startsWith("photo") ->
                SfVectorGlyphs.PhotoLandscape
            canonical == ThazhSymbols.FileVideo || canonical.startsWith("film") || canonical.startsWith("video") ->
                SfVectorGlyphs.FilmClapper
            canonical == ThazhSymbols.FileAudio || canonical.startsWith("music.note") || canonical == "waveform" ->
                SfVectorGlyphs.MusicNotes
            canonical == ThazhSymbols.FileDocument || canonical == ThazhSymbols.FileOther ||
                canonical.startsWith("doc") ->
                SfVectorGlyphs.DocText
            canonical == ThazhSymbols.FileArchive || canonical.startsWith("archivebox") ->
                SfVectorGlyphs.ArchiveBox
            canonical == ThazhSymbols.FileApk || canonical.startsWith("shippingbox") ->
                SfVectorGlyphs.ShippingBoxCube

            // 7. Directional, Selection, Search & Toolbar Controls
            canonical == ThazhSymbols.SwapVert || canonical == "arrow.up.arrow.down" ->
                SfVectorGlyphs.ArrowUpArrowDown
            canonical == ThazhSymbols.ArrowUp || canonical.startsWith("arrow.up") ->
                SfVectorGlyphs.ArrowUp
            canonical == ThazhSymbols.ArrowDown || canonical.startsWith("arrow.down") ->
                SfVectorGlyphs.ArrowDown
            canonical == ThazhSymbols.Back || canonical == "chevron.left" || canonical.startsWith("chevron.backward") ->
                SfVectorGlyphs.ChevronLeft
            canonical == ThazhSymbols.ChevronRight || canonical == "chevron.right" || canonical.startsWith("chevron.forward") ->
                SfVectorGlyphs.ChevronRight
            canonical == ThazhSymbols.Close || canonical == "xmark" || canonical.startsWith("xmark.circle") ->
                SfVectorGlyphs.Xmark
            canonical == ThazhSymbols.Refresh || canonical.startsWith("arrow.clockwise") ->
                SfVectorGlyphs.ArrowClockwise
            canonical == ThazhSymbols.Search || canonical.startsWith("magnifyingglass") ->
                SfVectorGlyphs.MagnifyingGlass
            canonical == ThazhSymbols.Edit || canonical.startsWith("pencil") ->
                SfVectorGlyphs.PencilEdit
            canonical == ThazhSymbols.MoreHoriz || canonical.startsWith("ellipsis") ->
                SfVectorGlyphs.EllipsisCircle
            canonical == ThazhSymbols.Checklist || canonical == "checklist" ->
                SfVectorGlyphs.Checklist
            canonical == ThazhSymbols.Sort || canonical.startsWith("line.3.horizontal") ->
                SfVectorGlyphs.FilterLines
            canonical == ThazhSymbols.SelectAll || canonical == ThazhSymbols.CheckCircle ||
                canonical == "checkmark.circle.fill" ->
                SfVectorGlyphs.CheckCircleFill
            canonical == "checkmark" ->
                SfVectorGlyphs.Checkmark
            canonical == ThazhSymbols.RadioUnchecked || canonical == "circle" ->
                SfVectorGlyphs.CircleOutline
            canonical == ThazhSymbols.Globe || canonical.startsWith("globe") ->
                SfVectorGlyphs.GlobeGrid
            canonical.startsWith("exclamationmark") ->
                SfVectorGlyphs.WarningTriangle
            else -> SfVectorGlyphs.Sparkles
        }
    }
}

private object SfVectorGlyphs {
    private val strokeBrush = SolidColor(Color.White)

    private inline fun sfIcon(
        name: String,
        autoMirror: Boolean = false,
        block: ImageVector.Builder.() -> Unit,
    ): ImageVector = ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f,
        autoMirror = autoMirror,
    ).apply(block).build()

    // 1. PaperPlaneSend ("paperplane.fill") — Purpose: Send Files
    val PaperPlaneSend: ImageVector by lazy {
        sfIcon("sf_paperplane_send") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 3f)
                lineTo(10.2f, 13.8f)
                moveTo(21f, 3f)
                lineTo(14.5f, 21f)
                lineTo(10.2f, 13.8f)
                lineTo(3f, 9.5f)
                close()
            }
        }
    }

    // 2. RadiowavesLeftRight ("dot.radiowaves.left.and.right") — Purpose: Wi-Fi Direct / P2P Broadcast
    val RadiowavesLeftRight: ImageVector by lazy {
        sfIcon("sf_radiowaves_left_right") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Center emitter circle
                moveTo(13.6f, 12f)
                curveTo(13.6f, 12.88f, 12.88f, 13.6f, 12f, 13.6f)
                curveTo(11.12f, 13.6f, 10.4f, 12.88f, 10.4f, 12f)
                curveTo(10.4f, 11.12f, 11.12f, 10.4f, 12f, 10.4f)
                curveTo(12.88f, 10.4f, 13.6f, 11.12f, 13.6f, 12f)
                close()
                // Inner left & right wave arcs
                moveTo(8.2f, 8.4f)
                quadTo(5.6f, 12f, 8.2f, 15.6f)
                moveTo(15.8f, 8.4f)
                quadTo(18.4f, 12f, 15.8f, 15.6f)
                // Outer left & right wave arcs
                moveTo(5.0f, 5.4f)
                quadTo(1.2f, 12f, 5.0f, 18.6f)
                moveTo(19.0f, 5.4f)
                quadTo(22.8f, 12f, 19.0f, 18.6f)
            }
        }
    }

    // 3. ArrowDownCircle ("arrow.down.circle.fill") — Purpose: Receive Tab & Download
    val ArrowDownCircle: ImageVector by lazy {
        sfIcon("sf_arrow_down_circle") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                close()
                moveTo(12f, 7.8f)
                lineTo(12f, 15.6f)
                moveTo(8.6f, 12.6f)
                lineTo(12f, 16f)
                lineTo(15.4f, 12.6f)
            }
        }
    }

    // 4. TrayArrowDown ("square.and.arrow.down") — Purpose: Save to Downloads
    val TrayArrowDown: ImageVector by lazy {
        sfIcon("sf_tray_arrow_down") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3.5f)
                lineTo(12f, 14.5f)
                moveTo(8.2f, 11f)
                lineTo(12f, 14.8f)
                lineTo(15.8f, 11f)
                moveTo(4f, 13.5f)
                lineTo(4f, 18.5f)
                quadTo(4f, 20f, 5.5f, 20f)
                lineTo(18.5f, 20f)
                quadTo(20f, 20f, 20f, 18.5f)
                lineTo(20f, 13.5f)
            }
        }
    }

    // 5. CloudUpload ("icloud.and.arrow.up") — Purpose: Link Share & Cloud Upload
    val CloudUpload: ImageVector by lazy {
        sfIcon("sf_cloud_upload") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6.5f, 18.5f)
                curveTo(4.0f, 18.5f, 2.2f, 16.7f, 2.2f, 14.3f)
                curveTo(2.2f, 12.2f, 3.7f, 10.5f, 5.7f, 10.2f)
                curveTo(6.4f, 6.8f, 9.4f, 4.5f, 12.8f, 4.5f)
                curveTo(16.6f, 4.5f, 19.7f, 7.4f, 20.0f, 11.1f)
                curveTo(21.3f, 11.7f, 22.0f, 13.0f, 22.0f, 14.6f)
                curveTo(22.0f, 16.8f, 20.2f, 18.5f, 18.0f, 18.5f)
                lineTo(15.5f, 18.5f)
                // Upward upload arrow
                moveTo(12f, 20f)
                lineTo(12f, 11.2f)
                moveTo(9.2f, 13.8f)
                lineTo(12f, 11f)
                lineTo(14.8f, 13.8f)
            }
        }
    }

    // 6. CloudCheckmark ("checkmark.icloud") — Purpose: Cloud Synced Account
    val CloudCheckmark: ImageVector by lazy {
        sfIcon("sf_cloud_checkmark") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6.5f, 18.5f)
                curveTo(4.0f, 18.5f, 2.2f, 16.7f, 2.2f, 14.3f)
                curveTo(2.2f, 12.2f, 3.7f, 10.5f, 5.7f, 10.2f)
                curveTo(6.4f, 6.8f, 9.4f, 4.5f, 12.8f, 4.5f)
                curveTo(16.6f, 4.5f, 19.7f, 7.4f, 20.0f, 11.1f)
                curveTo(21.3f, 11.7f, 22.0f, 13.0f, 22.0f, 14.6f)
                curveTo(22.0f, 16.8f, 20.2f, 18.5f, 18.0f, 18.5f)
                close()
                moveTo(9.2f, 13.2f)
                lineTo(11.3f, 15.3f)
                lineTo(15.2f, 11.0f)
            }
        }
    }

    // 7. FolderFill ("folder.fill") — Purpose: Received Files Tab & Empty State
    val FolderFill: ImageVector by lazy {
        sfIcon("sf_folder_fill") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 7f)
                quadTo(3f, 5f, 5f, 5f)
                lineTo(9.2f, 5f)
                 lineTo(11.4f, 7.4f)
                lineTo(19f, 7.4f)
                quadTo(21f, 7.4f, 21f, 9.4f)
                lineTo(21f, 17.5f)
                quadTo(21f, 19.5f, 19f, 19.5f)
                lineTo(5f, 19.5f)
                quadTo(3f, 19.5f, 3f, 17.5f)
                close()
                moveTo(3.2f, 10.8f)
                lineTo(20.8f, 10.8f)
            }
        }
    }

    // 8. BluetoothWave ("dot.radiowaves.left.and.right.ble") — Purpose: BLE Fast Discovery
    val BluetoothWave: ImageVector by lazy {
        sfIcon("sf_bluetooth_wave") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(7.5f, 7.5f)
                lineTo(16.5f, 16.5f)
                lineTo(12f, 21f)
                lineTo(12f, 3f)
                lineTo(16.5f, 7.5f)
                lineTo(7.5f, 16.5f)
                // Subtle side BLE wave ticks
                moveTo(4.5f, 9.2f)
                quadTo(2.8f, 12f, 4.5f, 14.8f)
                moveTo(19.5f, 9.2f)
                quadTo(21.2f, 12f, 19.5f, 14.8f)
            }
        }
    }

    // 9. NfcWaveTag ("sensor.tag.radiowaves.forward") — Purpose: NFC Tap-to-Pair
    val NfcWaveTag: ImageVector by lazy {
        sfIcon("sf_nfc_wave_tag") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5.5f, 8.5f)
                quadTo(9.2f, 12f, 5.5f, 15.5f)
                moveTo(10f, 6f)
                quadTo(15.2f, 12f, 10f, 18f)
                moveTo(14.5f, 3.5f)
                quadTo(21.5f, 12f, 14.5f, 20.5f)
            }
        }
    }

    // 10. RadarCompass ("safari") — Purpose: Spatial Radar Empty State & Scan
    val RadarCompass: ImageVector by lazy {
        sfIcon("sf_radar_compass") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                close()
                // Inner radar ring & sweep needle
                moveTo(16.2f, 7.8f)
                lineTo(13.6f, 13.6f)
                lineTo(7.8f, 16.2f)
                lineTo(10.4f, 10.4f)
                close()
            }
        }
    }

    // 11. WifiSlash ("wifi.slash") — Purpose: Wi-Fi Disabled Warning
    val WifiSlash: ImageVector by lazy {
        sfIcon("sf_wifi_slash") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(2.5f, 8.8f)
                quadTo(12f, 1.8f, 21.5f, 8.8f)
                moveTo(6.0f, 12.5f)
                quadTo(12f, 8.0f, 18.0f, 12.5f)
                moveTo(9.5f, 16.0f)
                quadTo(12f, 14.2f, 14.5f, 16.0f)
                moveTo(12f, 19.2f)
                lineTo(12.01f, 19.2f)
                // Diagonal slash
                moveTo(4f, 4f)
                lineTo(20f, 20f)
            }
        }
    }

    // 12. EyeOpen ("eye") — Purpose: Always Visible Mode
    val EyeOpen: ImageVector by lazy {
        sfIcon("sf_eye_open") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(2.2f, 12f)
                quadTo(12f, 4.2f, 21.8f, 12f)
                quadTo(12f, 19.8f, 2.2f, 12f)
                close()
                moveTo(14.8f, 12f)
                curveTo(14.8f, 13.55f, 13.55f, 14.8f, 12f, 14.8f)
                curveTo(10.45f, 14.8f, 9.2f, 13.55f, 9.2f, 12f)
                curveTo(9.2f, 10.45f, 10.45f, 9.2f, 12f, 9.2f)
                curveTo(13.55f, 9.2f, 14.8f, 10.45f, 14.8f, 12f)
                close()
            }
        }
    }

    // 13. EyeSlash ("eye.slash") — Purpose: Hidden Visibility Mode
    val EyeSlash: ImageVector by lazy {
        sfIcon("sf_eye_slash") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(2.5f, 12f)
                quadTo(12f, 4.5f, 21.5f, 12f)
                quadTo(12f, 19.5f, 2.5f, 12f)
                moveTo(4f, 4f)
                lineTo(20f, 20f)
            }
        }
    }

    // 14. StopwatchTimer ("clock.fill" / "timer") — Purpose: 10-Minute Temporary Visibility
    val StopwatchTimer: ImageVector by lazy {
        sfIcon("sf_stopwatch_timer") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9.5f, 2.5f)
                lineTo(14.5f, 2.5f)
                moveTo(12f, 2.5f)
                lineTo(12f, 5f)
                moveTo(19.5f, 13f)
                curveTo(19.5f, 17.14f, 16.14f, 20.5f, 12f, 20.5f)
                curveTo(7.86f, 20.5f, 4.5f, 17.14f, 4.5f, 13f)
                curveTo(4.5f, 8.86f, 7.86f, 5.5f, 12f, 5.5f)
                curveTo(16.14f, 5.5f, 19.5f, 8.86f, 19.5f, 13f)
                close()
                moveTo(12f, 13f)
                lineTo(15.2f, 9.8f)
            }
        }
    }

    // 15. ClockHistory ("clock") — Purpose: Recent / Saved Device Timestamp
    val ClockHistory: ImageVector by lazy {
        sfIcon("sf_clock_history") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                close()
                moveTo(12f, 7.2f)
                lineTo(12f, 12f)
                lineTo(15.4f, 14.2f)
            }
        }
    }

    // 16. InfinityLoop ("infinity") — Purpose: Unlimited Visibility Mode
    val InfinityLoop: ImageVector by lazy {
        sfIcon("sf_infinity_loop") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 12f)
                curveTo(9.5f, 8f, 3.5f, 8f, 3.5f, 12f)
                curveTo(3.5f, 16f, 9.5f, 16f, 12f, 12f)
                curveTo(14.5f, 8f, 20.5f, 8f, 20.5f, 12f)
                curveTo(20.5f, 16f, 14.5f, 16f, 12f, 12f)
            }
        }
    }

    // 17. ShieldCheck ("checkmark.shield") — Purpose: Receive Security & Confirmation
    val ShieldCheck: ImageVector by lazy {
        sfIcon("sf_shield_check") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 2.8f)
                lineTo(19f, 5.8f)
                lineTo(19f, 11.5f)
                quadTo(19f, 17.8f, 12f, 21.2f)
                quadTo(5f, 17.8f, 5f, 11.5f)
                lineTo(5f, 5.8f)
                close()
                moveTo(9f, 12.2f)
                lineTo(11.2f, 14.4f)
                lineTo(15.4f, 9.8f)
            }
        }
    }

    // 18. BellWave ("bell.badge") — Purpose: Notification & Sound Alert
    val BellWave: ImageVector by lazy {
        sfIcon("sf_bell_wave") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18f, 9.5f)
                curveTo(18f, 6.2f, 15.3f, 3.5f, 12f, 3.5f)
                curveTo(8.7f, 3.5f, 6f, 6.2f, 6f, 9.5f)
                lineTo(6f, 14.2f)
                lineTo(4.2f, 16.5f)
                lineTo(19.8f, 16.5f)
                lineTo(18f, 14.2f)
                close()
                moveTo(10f, 19.5f)
                quadTo(12f, 21.2f, 14f, 19.5f)
            }
        }
    }

    // 19. SquareOnSquare ("square.on.square.dashed") — Purpose: Overlay Dialog & Transparency
    val SquareOnSquare: ImageVector by lazy {
        sfIcon("sf_square_on_square") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 4f); lineTo(20f, 4f); lineTo(20f, 15f); lineTo(9f, 15f); close()
                moveTo(4f, 9f); lineTo(15f, 9f); lineTo(15f, 20f); lineTo(4f, 20f); close()
            }
        }
    }

    // 20. PersonCropCircle ("person.crop.circle") — Purpose: Account & Profile
    val PersonCropCircle: ImageVector by lazy {
        sfIcon("sf_person_crop_circle") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                close()
                // Head circle
                moveTo(14.8f, 9.8f)
                curveTo(14.8f, 11.35f, 13.55f, 12.6f, 12f, 12.6f)
                curveTo(10.45f, 12.6f, 9.2f, 11.35f, 9.2f, 9.8f)
                curveTo(9.2f, 8.25f, 10.45f, 7.0f, 12f, 7.0f)
                curveTo(13.55f, 7.0f, 14.8f, 8.25f, 14.8f, 9.8f)
                close()
                // Shoulders arc
                moveTo(6.8f, 18.5f)
                quadTo(12f, 14.2f, 17.2f, 18.5f)
            }
        }
    }

    // 21. PersonSilhouette ("person.fill") — Purpose: Device Display Name
    val PersonSilhouette: ImageVector by lazy {
        sfIcon("sf_person_silhouette") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(15.5f, 8f)
                curveTo(15.5f, 9.93f, 13.93f, 11.5f, 12f, 11.5f)
                curveTo(10.07f, 11.5f, 8.5f, 9.93f, 8.5f, 8f)
                curveTo(8.5f, 6.07f, 10.07f, 4.5f, 12f, 4.5f)
                curveTo(13.93f, 4.5f, 15.5f, 6.07f, 15.5f, 8f)
                close()
                moveTo(5f, 19.5f)
                curveTo(5f, 15.8f, 8.1f, 14f, 12f, 14f)
                curveTo(15.9f, 14f, 19f, 15.8f, 19f, 19.5f)
                close()
            }
        }
    }

    // 22. LaptopAndIphone ("laptopcomputer.and.iphone") — Purpose: Connected / Saved Devices
    val LaptopAndIphone: ImageVector by lazy {
        sfIcon("sf_laptop_and_iphone") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 6f)
                lineTo(16f, 6f)
                lineTo(16f, 16f)
                lineTo(3f, 16f)
                close()
                moveTo(2f, 19f)
                lineTo(13f, 19f)
                moveTo(15f, 9f)
                lineTo(21f, 9f)
                lineTo(21f, 19f)
                lineTo(15f, 19f)
                close()
            }
        }
    }

    // 23. PortraitArrowRight ("rectangle.portrait.and.arrow.right") — Purpose: Sign Out
    val PortraitArrowRight: ImageVector by lazy {
        sfIcon("sf_portrait_arrow_right", autoMirror = true) {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(13f, 4.5f)
                lineTo(5.5f, 4.5f)
                quadTo(4f, 4.5f, 4f, 6f)
                lineTo(4f, 18f)
                quadTo(4f, 19.5f, 5.5f, 19.5f)
                lineTo(13f, 19.5f)
                moveTo(10f, 12f)
                lineTo(20.5f, 12f)
                moveTo(17f, 8.5f)
                lineTo(20.5f, 12f)
                lineTo(17f, 15.5f)
            }
        }
    }

    // 24. LinkChain ("link") — Purpose: Share Link URL
    val LinkChain: ImageVector by lazy {
        sfIcon("sf_link_chain") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10f, 13f)
                curveTo(11.5f, 14.8f, 14.2f, 14.8f, 15.8f, 13.2f)
                lineTo(18.5f, 10.5f)
                curveTo(20.2f, 8.8f, 20.2f, 6f, 18.5f, 4.3f)
                curveTo(16.8f, 2.6f, 14f, 2.6f, 12.3f, 4.3f)
                lineTo(10.8f, 5.8f)

                moveTo(14f, 11f)
                curveTo(12.5f, 9.2f, 9.8f, 9.2f, 8.2f, 10.8f)
                lineTo(5.5f, 13.5f)
                curveTo(3.8f, 15.2f, 3.8f, 18f, 5.5f, 19.7f)
                curveTo(7.2f, 21.4f, 10f, 21.4f, 11.7f, 19.7f)
                lineTo(13.2f, 18.2f)
            }
        }
    }

    // 25. LinkOff ("link.badge.minus") — Purpose: Disconnect Session
    val LinkOff: ImageVector by lazy {
        sfIcon("sf_link_off") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(10.8f, 5.8f)
                lineTo(12.3f, 4.3f)
                curveTo(14f, 2.6f, 16.8f, 2.6f, 18.5f, 4.3f)
                curveTo(20.2f, 6f, 20.2f, 8.8f, 18.5f, 10.5f)
                lineTo(16.8f, 12.2f)

                moveTo(8.2f, 10.8f)
                lineTo(5.5f, 13.5f)
                curveTo(3.8f, 15.2f, 3.8f, 18f, 5.5f, 19.7f)
                curveTo(7.2f, 21.4f, 10f, 21.4f, 11.7f, 19.7f)
                lineTo(13.2f, 18.2f)

                moveTo(4f, 4f)
                lineTo(20f, 20f)
            }
        }
    }

    // 26. DocOnDoc ("doc.on.doc") — Purpose: Copy to Clipboard
    val DocOnDoc: ImageVector by lazy {
        sfIcon("sf_doc_on_doc") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 7f)
                lineTo(18f, 7f)
                quadTo(19.5f, 7f, 19.5f, 8.5f)
                lineTo(19.5f, 19f)
                quadTo(19.5f, 20.5f, 18f, 20.5f)
                lineTo(9f, 20.5f)
                quadTo(7.5f, 20.5f, 7.5f, 19f)
                lineTo(7.5f, 8.5f)
                quadTo(7.5f, 7f, 9f, 7f)
                close()
                moveTo(16f, 7f)
                lineTo(16f, 5f)
                quadTo(16f, 3.5f, 14.5f, 3.5f)
                lineTo(6f, 3.5f)
                quadTo(4.5f, 3.5f, 4.5f, 5f)
                lineTo(4.5f, 15.5f)
                quadTo(4.5f, 17f, 6f, 17f)
                lineTo(7.5f, 17f)
            }
        }
    }

    // 27. QrCodeViewfinder ("qrcode") — Purpose: QR Code Preview
    val QrCodeViewfinder: ImageVector by lazy {
        sfIcon("sf_qrcode_viewfinder") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 4f); lineTo(10f, 4f); lineTo(10f, 10f); lineTo(4f, 10f); close()
                moveTo(14f, 4f); lineTo(20f, 4f); lineTo(20f, 10f); lineTo(14f, 10f); close()
                moveTo(4f, 14f); lineTo(10f, 14f); lineTo(10f, 20f); lineTo(4f, 20f); close()
                moveTo(14f, 14f); lineTo(17f, 14f); lineTo(17f, 17f); lineTo(14f, 17f); close()
                moveTo(19f, 17f); lineTo(20f, 17f); lineTo(20f, 20f); lineTo(17f, 20f)
            }
        }
    }

    // 28. ArrowUpRightSquare ("arrow.up.right.square") — Purpose: Open External URL / File
    val ArrowUpRightSquare: ImageVector by lazy {
        sfIcon("sf_arrow_up_right_square") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(13f, 4f)
                lineTo(20f, 4f)
                lineTo(20f, 11f)
                moveTo(20f, 4f)
                lineTo(10f, 14f)
                moveTo(19f, 14f)
                lineTo(19f, 19f)
                lineTo(5f, 19f)
                lineTo(5f, 5f)
                lineTo(10f, 5f)
            }
        }
    }

    // 29. ShareBoxArrowUp ("square.and.arrow.up") — Purpose: iOS System Share Action
    val ShareBoxArrowUp: ImageVector by lazy {
        sfIcon("sf_share_box_arrow_up") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 14.5f)
                lineTo(12f, 3.2f)
                moveTo(8.4f, 6.8f)
                lineTo(12f, 3.2f)
                lineTo(15.6f, 6.8f)
                moveTo(8f, 9.5f)
                lineTo(6f, 9.5f)
                quadTo(4.5f, 9.5f, 4.5f, 11f)
                lineTo(4.5f, 19f)
                quadTo(4.5f, 20.5f, 6f, 20.5f)
                lineTo(18f, 20.5f)
                quadTo(19.5f, 20.5f, 19.5f, 19f)
                lineTo(19.5f, 11f)
                quadTo(19.5f, 9.5f, 18f, 9.5f)
                lineTo(16f, 9.5f)
            }
        }
    }

    // 30. Gearshape ("gear") — Purpose: Settings
    val Gearshape: ImageVector by lazy {
        sfIcon("sf_gearshape") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Center hub ring
                moveTo(15.2f, 12f)
                curveTo(15.2f, 13.77f, 13.77f, 15.2f, 12f, 15.2f)
                curveTo(10.23f, 15.2f, 8.8f, 13.77f, 8.8f, 12f)
                curveTo(8.8f, 10.23f, 10.23f, 8.8f, 12f, 8.8f)
                curveTo(13.77f, 8.8f, 15.2f, 10.23f, 15.2f, 12f)
                close()
                // Outer gear body with 8 symmetric teeth
                moveTo(12f, 2.8f); lineTo(12f, 4.8f)
                moveTo(12f, 19.2f); lineTo(12f, 21.2f)
                moveTo(2.8f, 12f); lineTo(4.8f, 12f)
                moveTo(19.2f, 12f); lineTo(21.2f, 12f)
                moveTo(5.5f, 5.5f); lineTo(7.0f, 7.0f)
                moveTo(17.0f, 17.0f); lineTo(18.5f, 18.5f)
                moveTo(18.5f, 5.5f); lineTo(17.0f, 7.0f)
                moveTo(7.0f, 17.0f); lineTo(5.5f, 18.5f)
                moveTo(19.2f, 12f)
                curveTo(19.2f, 15.98f, 15.98f, 19.2f, 12f, 19.2f)
                curveTo(8.02f, 19.2f, 4.8f, 15.98f, 4.8f, 12f)
                curveTo(4.8f, 8.02f, 8.02f, 4.8f, 12f, 4.8f)
                curveTo(15.98f, 4.8f, 19.2f, 8.02f, 19.2f, 12f)
                close()
            }
        }
    }

    // 31. Sparkles ("sparkles") — Purpose: Liquid Glass & Appearance
    val Sparkles: ImageVector by lazy {
        sfIcon("sf_sparkles") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                // Primary 4-point star
                moveTo(11f, 3.5f)
                quadTo(11f, 9.5f, 17f, 9.5f)
                quadTo(11f, 9.5f, 11f, 15.5f)
                quadTo(11f, 9.5f, 5f, 9.5f)
                quadTo(11f, 9.5f, 11f, 3.5f)
                close()
                // Bottom-right sparkle
                moveTo(17.5f, 14f)
                lineTo(17.5f, 20f)
                moveTo(14.5f, 17f)
                lineTo(20.5f, 17f)
                // Top-right dot sparkle
                moveTo(18.5f, 4.5f)
                lineTo(18.5f, 7.5f)
                moveTo(17f, 6f)
                lineTo(20f, 6f)
            }
        }
    }

    // 32. CircleLeftHalfFilled ("circle.lefthalf.filled") — Purpose: System Theme Mode
    val CircleLeftHalfFilled: ImageVector by lazy {
        sfIcon("sf_circle_half") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                close()
                moveTo(12f, 3f)
                lineTo(12f, 21f)
                moveTo(9f, 5f); lineTo(9f, 19f)
                moveTo(6.2f, 7.5f); lineTo(6.2f, 16.5f)
            }
        }
    }

    // 33. SunMax ("sun.max.fill") — Purpose: Light Theme Mode
    val SunMax: ImageVector by lazy {
        sfIcon("sf_sun_max") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(16.2f, 12f)
                curveTo(16.2f, 14.32f, 14.32f, 16.2f, 12f, 16.2f)
                curveTo(9.68f, 16.2f, 7.8f, 14.32f, 7.8f, 12f)
                curveTo(7.8f, 9.68f, 9.68f, 7.8f, 12f, 7.8f)
                curveTo(14.32f, 7.8f, 16.2f, 9.68f, 16.2f, 12f)
                close()
                moveTo(12f, 2.5f); lineTo(12f, 4.8f)
                moveTo(12f, 19.2f); lineTo(12f, 21.5f)
                moveTo(2.5f, 12f); lineTo(4.8f, 12f)
                moveTo(19.2f, 12f); lineTo(21.5f, 12f)
                moveTo(5.3f, 5.3f); lineTo(7.0f, 7.0f)
                moveTo(17.0f, 17.0f); lineTo(18.7f, 18.7f)
                moveTo(18.7f, 5.3f); lineTo(17.0f, 7.0f)
                moveTo(7.0f, 17.0f); lineTo(5.3f, 18.7f)
            }
        }
    }

    // 34. MoonCrescent ("moon.fill") — Purpose: Dark Theme Mode
    val MoonCrescent: ImageVector by lazy {
        sfIcon("sf_moon_crescent") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(20.2f, 14.5f)
                curveTo(18.8f, 18.2f, 15.2f, 20.5f, 11.0f, 20.2f)
                curveTo(6.2f, 19.8f, 2.8f, 15.6f, 3.5f, 10.8f)
                curveTo(4.0f, 7.0f, 6.8f, 4.0f, 10.5f, 3.5f)
                curveTo(9.0f, 6.2f, 9.2f, 9.8f, 11.5f, 12.2f)
                curveTo(13.8f, 14.5f, 17.4f, 15.0f, 20.2f, 14.5f)
                close()
            }
        }
    }

    // 35. InternalDrive ("internaldrive") — Purpose: Storage & Data Category
    val InternalDrive: ImageVector by lazy {
        sfIcon("sf_internal_drive") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 6f)
                lineTo(20f, 6f)
                lineTo(21f, 18f)
                lineTo(3f, 18f)
                close()
                moveTo(3.5f, 13.5f)
                lineTo(20.5f, 13.5f)
                moveTo(16.5f, 16f)
                lineTo(17.5f, 16f)
            }
        }
    }

    // 36. SparkleBrush ("paintbrush") — Purpose: Clear Temp Staging Cache
    val SparkleBrush: ImageVector by lazy {
        sfIcon("sf_sparkle_brush") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18.5f, 3.5f)
                lineTo(20.5f, 5.5f)
                lineTo(11.5f, 14.5f)
                lineTo(9.5f, 12.5f)
                close()
                moveTo(9.5f, 12.5f)
                quadTo(5.0f, 13.0f, 4.0f, 19.5f)
                quadTo(10.5f, 18.5f, 11.5f, 14.5f)
                // Clean sparkle
                moveTo(5.5f, 5f); lineTo(5.5f, 8f)
                moveTo(4f, 6.5f); lineTo(7f, 6.5f)
            }
        }
    }

    // 37. TrashSweep ("trash.slash") — Purpose: Clear All Saved Devices
    val TrashSweep: ImageVector by lazy {
        sfIcon("sf_trash_sweep") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 6.5f); lineTo(16f, 6.5f)
                moveTo(7.5f, 6.5f); lineTo(8.2f, 4.2f); lineTo(11.8f, 4.2f); lineTo(12.5f, 6.5f)
                moveTo(5.5f, 6.5f); lineTo(6.2f, 19.5f); lineTo(13.8f, 19.5f); lineTo(14.5f, 6.5f)
                // Sweep motion lines
                moveTo(17.5f, 10f); lineTo(21f, 10f)
                moveTo(17f, 14f); lineTo(20.5f, 14f)
                moveTo(16.5f, 18f); lineTo(19.5f, 18f)
            }
        }
    }

    // 38. TrashCan ("trash") — Purpose: Delete File / Link
    val TrashCan: ImageVector by lazy {
        sfIcon("sf_trash_can") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4.5f, 6.5f); lineTo(19.5f, 6.5f)
                moveTo(9f, 6.5f); lineTo(9.6f, 4f); lineTo(14.4f, 4f); lineTo(15f, 6.5f)
                moveTo(6.5f, 6.5f)
                lineTo(7.4f, 19.2f)
                quadTo(7.5f, 20.5f, 8.8f, 20.5f)
                lineTo(15.2f, 20.5f)
                quadTo(16.5f, 20.5f, 16.6f, 19.2f)
                lineTo(17.5f, 6.5f)
                moveTo(10.2f, 10.5f); lineTo(10.5f, 16.8f)
                moveTo(13.8f, 10.5f); lineTo(13.5f, 16.8f)
            }
        }
    }

    // 39. BatteryBolt ("battery.100.bolt") — Purpose: Background Battery Optimization
    val BatteryBolt: ImageVector by lazy {
        sfIcon("sf_battery_bolt") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3f, 7f)
                lineTo(18f, 7f)
                lineTo(18f, 17f)
                lineTo(3f, 17f)
                close()
                moveTo(20.5f, 10f)
                lineTo(20.5f, 14f)
                moveTo(11.5f, 9f)
                lineTo(9.5f, 12f)
                lineTo(12.2f, 12f)
                lineTo(10.2f, 15f)
            }
        }
    }

    // 40. InfoCircle ("info.circle") — Purpose: About App & Details
    val InfoCircle: ImageVector by lazy {
        sfIcon("sf_info_circle") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                close()
                moveTo(12f, 11.2f)
                lineTo(12f, 16.2f)
                moveTo(12f, 8.0f)
                lineTo(12.01f, 8.0f)
            }
        }
    }

    // 41. Paperclip ("paperclip") — Purpose: Pick Any Files
    val Paperclip: ImageVector by lazy {
        sfIcon("sf_paperclip") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(18.5f, 11.2f)
                lineTo(11f, 18.7f)
                curveTo(9f, 20.7f, 5.8f, 20.7f, 3.8f, 18.7f)
                curveTo(1.8f, 16.7f, 1.8f, 13.5f, 3.8f, 11.5f)
                lineTo(12.2f, 3.1f)
                curveTo(13.6f, 1.7f, 15.8f, 1.7f, 17.2f, 3.1f)
                curveTo(18.6f, 4.5f, 18.6f, 6.7f, 17.2f, 8.1f)
                lineTo(9.2f, 16.1f)
                curveTo(8.5f, 16.8f, 7.3f, 16.8f, 6.6f, 16.1f)
                curveTo(5.9f, 15.4f, 5.9f, 14.2f, 6.6f, 13.5f)
                lineTo(14f, 6.1f)
            }
        }
    }

    // 42. PhotoLandscape ("photo.fill") — Purpose: Pick Photos/Videos & Image Category
    val PhotoLandscape: ImageVector by lazy {
        sfIcon("sf_photo_landscape") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5.5f, 4.5f)
                lineTo(18.5f, 4.5f)
                quadTo(20.5f, 4.5f, 20.5f, 6.5f)
                lineTo(20.5f, 17.5f)
                quadTo(20.5f, 19.5f, 18.5f, 19.5f)
                lineTo(5.5f, 19.5f)
                quadTo(3.5f, 19.5f, 3.5f, 17.5f)
                lineTo(3.5f, 6.5f)
                quadTo(3.5f, 4.5f, 5.5f, 4.5f)
                close()
                // Sun circle
                moveTo(9.8f, 9f)
                curveTo(9.8f, 9.77f, 9.17f, 10.4f, 8.4f, 10.4f)
                curveTo(7.63f, 10.4f, 7.0f, 9.77f, 7.0f, 9f)
                curveTo(7.0f, 8.23f, 7.63f, 7.6f, 8.4f, 7.6f)
                curveTo(9.17f, 7.6f, 9.8f, 8.23f, 9.8f, 9f)
                close()
                // Mountain peaks
                moveTo(4f, 17.5f)
                lineTo(9.5f, 12.5f)
                lineTo(13.2f, 15.8f)
                lineTo(16.2f, 13.2f)
                lineTo(20f, 17.2f)
            }
        }
    }

    // 43. FilmClapper ("film.fill") — Purpose: Video Category
    val FilmClapper: ImageVector by lazy {
        sfIcon("sf_film_clapper") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 5f); lineTo(20f, 5f); lineTo(20f, 19f); lineTo(4f, 19f); close()
                moveTo(10f, 9.2f); lineTo(15.2f, 12f); lineTo(10f, 14.8f); close()
            }
        }
    }

    // 44. MusicNotes ("music.note") — Purpose: Audio Category
    val MusicNotes: ImageVector by lazy {
        sfIcon("sf_music_notes") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 17.5f)
                lineTo(9f, 6.2f)
                lineTo(19f, 4.2f)
                lineTo(19f, 15.5f)
                moveTo(9f, 9.5f)
                lineTo(19f, 7.5f)
                // Left note head
                moveTo(9f, 17.5f)
                curveTo(9f, 18.88f, 7.88f, 20f, 6.5f, 20f)
                curveTo(5.12f, 20f, 4f, 18.88f, 4f, 17.5f)
                curveTo(4f, 16.12f, 5.12f, 15f, 6.5f, 15f)
                curveTo(7.88f, 15f, 9f, 16.12f, 9f, 17.5f)
                close()
                // Right note head
                moveTo(19f, 15.5f)
                curveTo(19f, 16.88f, 17.88f, 18f, 16.5f, 18f)
                curveTo(15.12f, 18f, 14f, 16.88f, 14f, 15.5f)
                curveTo(14f, 14.12f, 15.12f, 13f, 16.5f, 13f)
                curveTo(17.88f, 13f, 19f, 14.12f, 19f, 15.5f)
                close()
            }
        }
    }

    // 45. DocText ("doc.text.fill") — Purpose: Document & License Category
    val DocText: ImageVector by lazy {
        sfIcon("sf_doc_text") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6f, 3.5f)
                lineTo(14f, 3.5f)
                lineTo(18.5f, 8f)
                lineTo(18.5f, 20.5f)
                lineTo(6f, 20.5f)
                close()
                moveTo(13.5f, 3.5f); lineTo(13.5f, 8.5f); lineTo(18.5f, 8.5f)
                moveTo(9f, 12.5f); lineTo(15.5f, 12.5f)
                moveTo(9f, 16f); lineTo(13.5f, 16f)
            }
        }
    }

    // 46. ArchiveBox ("archivebox.fill") — Purpose: Archive Category
    val ArchiveBox: ImageVector by lazy {
        sfIcon("sf_archive_box") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(3.5f, 5f); lineTo(20.5f, 5f); lineTo(20.5f, 9f); lineTo(3.5f, 9f); close()
                moveTo(5f, 9f); lineTo(5f, 19.5f); lineTo(19f, 19.5f); lineTo(19f, 9f)
                moveTo(10f, 13f); lineTo(14f, 13f)
            }
        }
    }

    // 47. ShippingBoxCube ("shippingbox.fill") — Purpose: APK & Package Name
    val ShippingBoxCube: ImageVector by lazy {
        sfIcon("sf_shipping_box_cube") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3f)
                lineTo(20f, 7.5f)
                lineTo(20f, 16.5f)
                lineTo(12f, 21f)
                lineTo(4f, 16.5f)
                lineTo(4f, 7.5f)
                close()
                moveTo(4f, 7.5f); lineTo(12f, 12f); lineTo(20f, 7.5f)
                moveTo(12f, 12f); lineTo(12f, 21f)
            }
        }
    }

    // 48. ArrowUpArrowDown ("arrow.up.arrow.down") — Purpose: Bidirectional File Transfer
    val ArrowUpArrowDown: ImageVector by lazy {
        sfIcon("sf_arrow_up_arrow_down") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(8f, 19f); lineTo(8f, 5f)
                moveTo(4.5f, 8.5f); lineTo(8f, 5f); lineTo(11.5f, 8.5f)
                moveTo(16f, 5f); lineTo(16f, 19f)
                moveTo(12.5f, 15.5f); lineTo(16f, 19f); lineTo(19.5f, 15.5f)
            }
        }
    }

    // 49. ArrowUp ("arrow.up") — Purpose: Sent Item Indicator
    val ArrowUp: ImageVector by lazy {
        sfIcon("sf_arrow_up") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 19.5f); lineTo(12f, 4.5f)
                moveTo(6.2f, 10.2f); lineTo(12f, 4.5f); lineTo(17.8f, 10.2f)
            }
        }
    }

    // 50. ArrowDown ("arrow.down") — Purpose: Received Item Indicator
    val ArrowDown: ImageVector by lazy {
        sfIcon("sf_arrow_down") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 4.5f); lineTo(12f, 19.5f)
                moveTo(6.2f, 13.8f); lineTo(12f, 19.5f); lineTo(17.8f, 13.8f)
            }
        }
    }

    // 51. ChevronLeft ("chevron.left") — Purpose: Back Navigation
    val ChevronLeft: ImageVector by lazy {
        sfIcon("sf_chevron_left", autoMirror = true) {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.3f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(15f, 5f)
                lineTo(8f, 12f)
                lineTo(15f, 19f)
            }
        }
    }

    // 52. ChevronRight ("chevron.right") — Purpose: Forward Disclosure
    val ChevronRight: ImageVector by lazy {
        sfIcon("sf_chevron_right", autoMirror = true) {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.3f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(9f, 5f)
                lineTo(16f, 12f)
                lineTo(9f, 19f)
            }
        }
    }

    // 53. Xmark ("xmark") — Purpose: Close / Cancel / Stop
    val Xmark: ImageVector by lazy {
        sfIcon("sf_xmark") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(6f, 6f); lineTo(18f, 18f)
                moveTo(18f, 6f); lineTo(6f, 18f)
            }
        }
    }

    // 54. ArrowClockwise ("arrow.clockwise") — Purpose: Retry / Refresh / Reset Timer
    val ArrowClockwise: ImageVector by lazy {
        sfIcon("sf_arrow_clockwise") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(19.5f, 5f)
                lineTo(19.5f, 9.8f)
                lineTo(14.7f, 9.8f)
                moveTo(19.2f, 9.5f)
                curveTo(17.9f, 6.2f, 14.7f, 4.2f, 11.2f, 4.5f)
                curveTo(6.8f, 4.9f, 3.6f, 8.6f, 4.0f, 13.0f)
                curveTo(4.4f, 17.4f, 8.2f, 20.4f, 12.6f, 20.0f)
                curveTo(16.0f, 19.7f, 18.8f, 17.4f, 19.6f, 14.2f)
            }
        }
    }

    // 55. MagnifyingGlass ("magnifyingglass") — Purpose: Floating Search Bar
    val MagnifyingGlass: ImageVector by lazy {
        sfIcon("sf_magnifyingglass") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(16.5f, 10.5f)
                curveTo(16.5f, 13.81f, 13.81f, 16.5f, 10.5f, 16.5f)
                curveTo(7.19f, 16.5f, 4.5f, 13.81f, 4.5f, 10.5f)
                curveTo(4.5f, 7.19f, 7.19f, 4.5f, 10.5f, 4.5f)
                curveTo(13.81f, 4.5f, 16.5f, 7.19f, 16.5f, 10.5f)
                close()
                moveTo(15.0f, 15.0f)
                lineTo(20.0f, 20.0f)
            }
        }
    }

    // 56. PencilEdit ("pencil") — Purpose: Rename File / Device / Edit Note
    val PencilEdit: ImageVector by lazy {
        sfIcon("sf_pencil_edit") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(16.5f, 3.8f)
                lineTo(20.2f, 7.5f)
                lineTo(8.2f, 19.5f)
                lineTo(4.0f, 20.2f)
                lineTo(4.7f, 16.0f)
                close()
                moveTo(14.0f, 6.3f)
                lineTo(17.7f, 10.0f)
            }
        }
    }

    // 57. EllipsisCircle ("ellipsis") — Purpose: Context Menu More Actions
    val EllipsisCircle: ImageVector by lazy {
        sfIcon("sf_ellipsis") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.8f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(5.5f, 12f); lineTo(5.6f, 12f)
                moveTo(12f, 12f); lineTo(12.1f, 12f)
                moveTo(18.5f, 12f); lineTo(18.6f, 12f)
            }
        }
    }

    // 58. Checklist ("checklist") — Purpose: Enter Multi-Selection Mode
    val Checklist: ImageVector by lazy {
        sfIcon("sf_checklist") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(4f, 7f); lineTo(6.2f, 9.2f); lineTo(9.5f, 5.5f)
                moveTo(12.5f, 7.5f); lineTo(20f, 7.5f)
                moveTo(4f, 16f); lineTo(6.2f, 18.2f); lineTo(9.5f, 14.5f)
                moveTo(12.5f, 16.5f); lineTo(20f, 16.5f)
            }
        }
    }

    // 59. FilterLines ("line.3.horizontal.decrease") — Purpose: Sort Order Menu
    val FilterLines: ImageVector by lazy {
        sfIcon("sf_filter_lines") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.1f,
                strokeLineCap = StrokeCap.Round,
            ) {
                moveTo(4f, 7f); lineTo(20f, 7f)
                moveTo(7f, 12f); lineTo(17f, 12f)
                moveTo(10f, 17f); lineTo(14f, 17f)
            }
        }
    }

    // 60. CheckCircleFill ("checkmark.circle.fill") — Purpose: Selected / Completed
    val CheckCircleFill: ImageVector by lazy {
        sfIcon("sf_check_circle") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                close()
                moveTo(8.5f, 12.2f)
                lineTo(11f, 14.7f)
                lineTo(15.8f, 9.6f)
            }
        }
    }

    // 61. Checkmark ("checkmark") — Purpose: Menu Active Item
    val Checkmark: ImageVector by lazy {
        sfIcon("sf_checkmark") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.3f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(5f, 12.5f)
                lineTo(10f, 17.5f)
                lineTo(19.5f, 6.5f)
            }
        }
    }

    // 62. CircleOutline ("circle") — Purpose: Unselected Radio Item
    val CircleOutline: ImageVector by lazy {
        sfIcon("sf_circle_outline") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                close()
            }
        }
    }

    // 63. WarningTriangle ("exclamationmark.triangle") — Purpose: Warning / Alert
    val WarningTriangle: ImageVector by lazy {
        sfIcon("sf_warning_triangle") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 2.0f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(12f, 3.5f)
                lineTo(21f, 19.5f)
                lineTo(3f, 19.5f)
                close()
                moveTo(12f, 9.5f); lineTo(12f, 14f)
                moveTo(12f, 16.8f); lineTo(12.01f, 16.8f)
            }
        }
    }

    // 64. GlobeGrid ("globe") — Purpose: In-App Web Browser & Language Settings
    val GlobeGrid: ImageVector by lazy {
        sfIcon("sf_globe_grid") {
            path(
                stroke = strokeBrush,
                strokeLineWidth = 1.9f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            ) {
                moveTo(21f, 12f)
                curveTo(21f, 16.97f, 16.97f, 21f, 12f, 21f)
                curveTo(7.03f, 21f, 3f, 16.97f, 3f, 12f)
                curveTo(3f, 7.03f, 7.03f, 3f, 12f, 3f)
                curveTo(16.97f, 3f, 21f, 7.03f, 21f, 12f)
                close()
                // Equator & latitude lines
                moveTo(3.4f, 12f); lineTo(20.6f, 12f)
                moveTo(4.6f, 8f); lineTo(19.4f, 8f)
                moveTo(4.6f, 16f); lineTo(19.4f, 16f)
                // Longitude curves
                moveTo(12f, 3f)
                curveTo(8.2f, 6.5f, 8.2f, 17.5f, 12f, 21f)
                moveTo(12f, 3f)
                curveTo(15.8f, 6.5f, 15.8f, 17.5f, 12f, 21f)
            }
        }
    }
}
