package com.thazh.share.ui.theme

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AllInclusive
import androidx.compose.material.icons.rounded.Android
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AttachFile
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatterySaver
import androidx.compose.material.icons.rounded.BlurOff
import androidx.compose.material.icons.rounded.BluetoothSearching
import androidx.compose.material.icons.rounded.BrightnessAuto
import androidx.compose.material.icons.rounded.Cancel
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloudDone
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Devices
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.FolderZip
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.InsertDriveFile
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LinkOff
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.Nfc
import androidx.compose.material.icons.rounded.NotificationsActive
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PhotoLibrary
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.Radar
import androidx.compose.material.icons.rounded.RadioButtonUnchecked
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SaveAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SelectAll
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.Sort
import androidx.compose.material.icons.rounded.Storage
import androidx.compose.material.icons.rounded.SwapVert
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VideoFile
import androidx.compose.material.icons.rounded.ViewSidebar
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material.icons.rounded.WifiOff
import androidx.compose.material.icons.rounded.WifiTethering
import androidx.compose.ui.graphics.vector.ImageVector
import io.github.neoruaa.glasmio.icons.GlasmioIconResolver
import io.github.neoruaa.glasmio.icons.MaterialGlasmioIconResolver
import io.github.neoruaa.glasmio.icons.SfSymbol

object ThazhSymbols {
    // Tab items map to SfSymbol enum constants (rendered cleanly via ThazhIconResolver)
    val TabSend = SfSymbol.RadioWaves
    val TabReceive = SfSymbol.DownloadFilled
    val TabLinkShare = SfSymbol.Cloud
    val TabReceivedFiles = SfSymbol.Layers

    const val WifiTethering = "antenna.radiowaves.left.and.right"
    const val ArrowUp = "arrow.up"
    const val ArrowDown = "arrow.down"
    const val FolderOpen = "folder"
    const val Settings = "gear"
    const val Person = "person"
    const val AccountCircle = "person.crop.circle"
    const val Logout = "rectangle.portrait.and.arrow.right"
    const val Bluetooth = "wave.3.right"
    const val Nfc = "sensor.tag.radiowaves.forward"
    const val BatterySaver = "battery.100.bolt"
    const val ReduceTransparency = "square.on.square.dashed"
    const val LiquidGlass = "sparkles"
    const val ThemeSystem = "circle.lefthalf.filled"
    const val ThemeLight = "sun.max.fill"
    const val ThemeDark = "moon.fill"
    const val Devices = "laptopcomputer.and.iphone"
    const val History = "clock"
    const val Radar = "dot.scope"
    const val WifiOff = "wifi.slash"
    const val MoreHoriz = "ellipsis"
    const val Edit = "pencil"
    const val Delete = "trash"
    const val Share = "square.and.arrow.up"
    const val Download = "arrow.down.to.line"
    const val Info = "info.circle"
    const val OpenInNew = "arrow.up.right.square"
    const val Close = "xmark"
    const val Refresh = "arrow.clockwise"
    const val Link = "link"
    const val LinkOff = "link.badge.minus"
    const val CloudUpload = "icloud.and.arrow.up"
    const val CloudDone = "checkmark.icloud"
    const val Copy = "doc.on.doc"
    const val QrCode = "qrcode"
    const val AttachFile = "paperclip"
    const val PhotoLibrary = "photo.on.rectangle"
    const val SwapVert = "arrow.up.arrow.down"
    const val Checklist = "checklist"
    const val Sort = "line.3.horizontal.decrease"
    const val SelectAll = "checkmark.circle.badge.questionmark"
    const val CheckCircle = "checkmark.circle.fill"
    const val RadioUnchecked = "circle"
    const val ChevronRight = "chevron.right"
    const val Back = "chevron.left"

    // Visibility, incoming dialog & extended settings symbols
    const val VisibilityOn = "eye"
    const val VisibilityOff = "eye.slash"
    const val Timer = "timer"
    const val AllInclusive = "infinity"
    const val ShieldCheck = "checkmark.shield"
    const val NotificationsActive = "bell.badge"
    const val SaveAlt = "square.and.arrow.down"
    const val Storage = "internaldrive"
    const val Cleaning = "paintbrush"
    const val DeleteSweep = "trash.slash"

    // File category symbols
    const val FileImage = "photo.fill"
    const val FileVideo = "film.fill"
    const val FileAudio = "music.note"
    const val FileDocument = "doc.text.fill"
    const val FileArchive = "archivebox.fill"
    const val FileApk = "shippingbox.fill"
    const val FileOther = "doc.fill"
}

object ThazhIconResolver : GlasmioIconResolver {
    override fun resolve(systemName: String): ImageVector = when (systemName) {
        SfSymbol.RadioWaves.systemName, ThazhSymbols.ArrowUp -> Icons.Rounded.ArrowUpward
        SfSymbol.DownloadFilled.systemName, ThazhSymbols.ArrowDown -> Icons.Rounded.ArrowDownward
        SfSymbol.Cloud.systemName, ThazhSymbols.Link -> Icons.Rounded.Link
        SfSymbol.Layers.systemName, ThazhSymbols.FolderOpen -> Icons.Rounded.FolderOpen
        ThazhSymbols.WifiTethering -> Icons.Rounded.WifiTethering
        ThazhSymbols.Settings, SfSymbol.Settings.systemName -> Icons.Rounded.Settings
        ThazhSymbols.Person -> Icons.Rounded.Person
        ThazhSymbols.AccountCircle -> Icons.Rounded.AccountCircle
        ThazhSymbols.Logout -> Icons.AutoMirrored.Rounded.Logout
        ThazhSymbols.Bluetooth -> Icons.Rounded.BluetoothSearching
        ThazhSymbols.Nfc -> Icons.Rounded.Nfc
        ThazhSymbols.BatterySaver -> Icons.Rounded.BatterySaver
        ThazhSymbols.ReduceTransparency -> Icons.Rounded.BlurOff
        ThazhSymbols.LiquidGlass -> Icons.Rounded.AutoAwesome
        ThazhSymbols.ThemeSystem -> Icons.Rounded.BrightnessAuto
        ThazhSymbols.ThemeLight -> Icons.Rounded.LightMode
        ThazhSymbols.ThemeDark -> Icons.Rounded.DarkMode
        ThazhSymbols.Devices -> Icons.Rounded.Devices
        ThazhSymbols.History -> Icons.Rounded.History
        ThazhSymbols.Radar -> Icons.Rounded.Radar
        ThazhSymbols.WifiOff -> Icons.Rounded.WifiOff
        ThazhSymbols.MoreHoriz -> Icons.Rounded.MoreHoriz
        ThazhSymbols.Edit -> Icons.Rounded.Edit
        ThazhSymbols.Delete -> Icons.Rounded.DeleteOutline
        ThazhSymbols.Share -> Icons.Rounded.Share
        ThazhSymbols.Download -> Icons.Rounded.Download
        ThazhSymbols.Info -> Icons.Rounded.Info
        ThazhSymbols.OpenInNew -> Icons.AutoMirrored.Rounded.OpenInNew
        ThazhSymbols.Close -> Icons.Rounded.Close
        ThazhSymbols.Refresh -> Icons.Rounded.Refresh
        ThazhSymbols.LinkOff -> Icons.Rounded.LinkOff
        ThazhSymbols.CloudUpload -> Icons.Rounded.CloudUpload
        ThazhSymbols.CloudDone -> Icons.Rounded.CloudDone
        ThazhSymbols.Copy -> Icons.Rounded.ContentCopy
        ThazhSymbols.QrCode -> Icons.Rounded.QrCode2
        ThazhSymbols.AttachFile -> Icons.Rounded.AttachFile
        ThazhSymbols.PhotoLibrary -> Icons.Rounded.PhotoLibrary
        ThazhSymbols.SwapVert -> Icons.Rounded.SwapVert
        ThazhSymbols.Checklist -> Icons.Rounded.Checklist
        ThazhSymbols.Sort -> Icons.Rounded.Sort
        ThazhSymbols.SelectAll -> Icons.Rounded.SelectAll
        ThazhSymbols.CheckCircle -> Icons.Rounded.CheckCircle
        ThazhSymbols.RadioUnchecked -> Icons.Rounded.RadioButtonUnchecked
        ThazhSymbols.ChevronRight, "chevron.forward" -> Icons.AutoMirrored.Rounded.KeyboardArrowRight
        ThazhSymbols.Back -> Icons.AutoMirrored.Rounded.ArrowBack
        ThazhSymbols.VisibilityOn -> Icons.Rounded.Visibility
        ThazhSymbols.VisibilityOff -> Icons.Rounded.VisibilityOff
        ThazhSymbols.Timer -> Icons.Rounded.Timer
        ThazhSymbols.AllInclusive -> Icons.Rounded.AllInclusive
        ThazhSymbols.ShieldCheck -> Icons.Rounded.VerifiedUser
        ThazhSymbols.NotificationsActive -> Icons.Rounded.NotificationsActive
        ThazhSymbols.SaveAlt -> Icons.Rounded.SaveAlt
        ThazhSymbols.Storage -> Icons.Rounded.Storage
        ThazhSymbols.Cleaning -> Icons.Rounded.CleaningServices
        ThazhSymbols.DeleteSweep -> Icons.Rounded.DeleteSweep
        ThazhSymbols.FileImage -> Icons.Rounded.Image
        ThazhSymbols.FileVideo -> Icons.Rounded.VideoFile
        ThazhSymbols.FileAudio -> Icons.Rounded.AudioFile
        ThazhSymbols.FileDocument -> Icons.Rounded.Description
        ThazhSymbols.FileArchive -> Icons.Rounded.FolderZip
        ThazhSymbols.FileApk -> Icons.Rounded.Android
        ThazhSymbols.FileOther -> Icons.Rounded.InsertDriveFile
        // Glasmio internal SF symbol names
        "checkmark" -> Icons.Rounded.Check
        "magnifyingglass" -> Icons.Rounded.Search
        "xmark.circle" -> Icons.Rounded.Cancel
        "sidebar.left" -> Icons.Rounded.ViewSidebar
        else -> MaterialGlasmioIconResolver.resolve(systemName)
    }
}
