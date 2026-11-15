package de.einsatzpulse.alarm

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.lifecycleScope
import com.google.firebase.FirebaseApp
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val navy=Color(0xFF0A1426)
private val panel=Color(0xFF17263D)
private val crimson=Color(0xFFFF5B65)
private val mint=Color(0xFF68DBC0)
private val soft=Color(0xFFABB9D0)
private val alarmColorScheme=darkColorScheme(primary=crimson,secondary=mint,
    background=navy,surface=panel,onSurface=Color.White,onBackground=Color.White)
private val r=RoundedCornerShape(20.dp)

class MainActivity:ComponentActivity() {
    override fun onCreate(savedInstanceState:Bundle?) {
        super.onCreate(savedInstanceState)
        val incident=intent.getIntExtra("incident_id",0)
        val unit=intent.getIntExtra("unit_id",0)
        val session=SecureSession(this)
        if(incident>0 && unit>0 && session.linked &&
            intent.getStringExtra("session_key")==session.fingerprint) {
            AlarmReceiptWorker.schedule(applicationContext,incident,unit,"opened")
        }
        setContent {
            MaterialTheme(colorScheme=alarmColorScheme) {
                AlarmApp(applicationContext)
            }
        }
    }
}

@Composable
private fun StateCard(label:String,value:String,modifier:Modifier=Modifier,color:Color=mint) {
    Card(modifier,shape=r,colors=CardDefaults.cardColors(containerColor=panel)) {
        Column(Modifier.padding(15.dp)) {
            Text(label,style=MaterialTheme.typography.labelSmall,color=soft)
            Spacer(Modifier.height(6.dp))
            Text(value,style=MaterialTheme.typography.headlineMedium,
                fontWeight=FontWeight.ExtraBold,color=color)
        }
    }
}
@Composable
private fun AlarmCard(item:AlarmIncident,busy:Boolean,onReply:(String)->Unit) {
    Card(shape=r,colors=CardDefaults.cardColors(containerColor=panel),
        modifier=Modifier.fillMaxWidth()) {
        Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement=Arrangement.SpaceBetween,modifier=Modifier.fillMaxWidth()) {
                Surface(color=if(item.status=="active")crimson.copy(alpha=.18f) else Color.DarkGray,
                    shape=RoundedCornerShape(10.dp)) {
                    Text(if(item.status=="active")"● AKTIVER EINSATZ" else "ABGESCHLOSSEN",
                        Modifier.padding(horizontal=10.dp,vertical=5.dp),color=if(item.status=="active")crimson else soft,
                        style=MaterialTheme.typography.labelSmall,fontWeight=FontWeight.Bold)
                }
                Text(item.unitName,color=soft,style=MaterialTheme.typography.labelSmall)
            }
            Text(item.keyword.ifEmpty { "Einsatzmeldung" },
                style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.ExtraBold)
            Text("#${item.number} · ${item.alarmAt.take(16)}",color=soft,
                style=MaterialTheme.typography.bodySmall)
            if(item.address.isNotBlank()) Row(verticalAlignment=Alignment.CenterVertically) {
                Icon(Icons.Default.LocationOn,null,tint=mint,modifier=Modifier.size(18.dp))
                Spacer(Modifier.width(5.dp))
                Text(item.address,style=MaterialTheme.typography.bodyMedium)
            }
            if(item.summary.isNotBlank()) Text(item.summary,color=soft,
                style=MaterialTheme.typography.bodyMedium)
            HorizontalDivider(color=Color.White.copy(alpha=.09f))
            Row(horizontalArrangement=Arrangement.SpaceBetween,modifier=Modifier.fillMaxWidth()) {
                Text("Kommen: ${item.yes}",color=mint,style=MaterialTheme.typography.labelMedium)
                Text("Später: ${item.delayed}",color=soft,style=MaterialTheme.typography.labelMedium)
                Text("Absagen: ${item.no}",color=soft,style=MaterialTheme.typography.labelMedium)
            }
            if(item.reply.isNotBlank()) Text("Meine Rückmeldung: ${when(item.reply) {
                "yes"->"Ich komme";"delayed"->"Später";"no"->"Ich komme nicht";else->"Unklar"
            }}",color=mint,style=MaterialTheme.typography.bodySmall)
            if(item.status=="active") {
                Row(horizontalArrangement=Arrangement.spacedBy(7.dp),modifier=Modifier.fillMaxWidth()) {
                    Button(onClick={onReply("yes")},enabled=!busy,modifier=Modifier.weight(1f),
                        contentPadding=PaddingValues(horizontal=6.dp)) { Text("Ich komme",maxLines=1) }
                    OutlinedButton(onClick={onReply("delayed")},enabled=!busy,
                        modifier=Modifier.weight(1f),contentPadding=PaddingValues(horizontal=4.dp)) {
                        Text("Später",maxLines=1) }
                    OutlinedButton(onClick={onReply("no")},enabled=!busy,
                        modifier=Modifier.weight(1.2f),contentPadding=PaddingValues(horizontal=3.dp)) {
                        Text("Nein",maxLines=1) }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlarmApp(context:Context) {
    val session=remember { SecureSession(context) }
    val api=remember { AlarmApi(context) }
    val snackbar=remember { SnackbarHostState() }
    val scope=rememberCoroutineScope()
    var linked by remember { mutableStateOf(session.linked) }
    var server by remember { mutableStateOf(session.server) }
    var code by remember { mutableStateOf("") }
    var label by remember { mutableStateOf(Build.MANUFACTURER+" "+Build.MODEL) }
    var busy by remember { mutableStateOf(false) }
    var replyBusy by remember { mutableStateOf("") }
    var tab by remember { mutableIntStateOf(0) }
    var dashboard by remember { mutableStateOf<AlarmDashboard?>(null) }
    var device by remember { mutableStateOf<AlarmDevice?>(null) }
    var error by remember { mutableStateOf("") }
    var selectedUnit by remember { mutableIntStateOf(0) }
    var diagnosticRefresh by remember { mutableIntStateOf(0) }
    var lastSync by remember { mutableStateOf("") }
    val notifications=rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        scope.launch { snackbar.showSnackbar(if(it)"Benachrichtigungen zugelassen" else "Alarmhinweise können blockiert sein") }
    }
    fun registerFcm() {
        if(!linked||FirebaseApp.getApps(context).isEmpty()) return
        runCatching {
            FirebaseMessaging.getInstance().token.addOnSuccessListener { token ->
                scope.launch {
                    runCatching { api.registerPushToken(token) }
                        .onFailure { error="FCM-Registrierung: ${it.message}" }
                    runCatching { device=api.device() }
                }
            }.addOnFailureListener { error="FCM-Konfiguration prüfen: ${it.message}" }
        }
    }
    fun refresh() {
        if(!linked)return
        scope.launch {
            try {
                device=api.device()
                dashboard=api.dashboard()
                lastSync=java.text.SimpleDateFormat("HH:mm:ss",java.util.Locale.GERMANY)
                    .format(java.util.Date())
                error=""
                if(device?.pushRegistered != true) registerFcm()
            } catch (e:Exception) { error=e.message ?: "Server derzeit nicht erreichbar" }
        }
    }
    LaunchedEffect(linked) {
        if(linked) {
            while(true) {
                refresh()
                delay(30000)
            }
        }
    }
    Scaffold(
        containerColor=navy,
        snackbarHost={SnackbarHost(snackbar)},
        topBar={
            TopAppBar(
                title={Column {
                    Text("EINSATZPULSE",fontWeight=FontWeight.Black,
                        style=MaterialTheme.typography.titleLarge)
                    Text("ALARM · NATIV",style=MaterialTheme.typography.labelSmall,color=mint)
                }},
                colors=TopAppBarDefaults.topAppBarColors(containerColor=navy),
                actions={
                    if(linked) IconButton(onClick={refresh()}) {
                        Icon(Icons.Default.Refresh,"Aktualisieren",tint=mint)
                    }
                })
        },
        bottomBar={
            if(linked) NavigationBar(containerColor=panel) {
                listOf(Triple("Alarme",Icons.Default.Notifications,0),
                    Triple("Einheiten",Icons.Default.Groups,1),
                    Triple("Gerät",Icons.Default.Settings,2)).forEach { (name,icon,index)->
                    NavigationBarItem(selected=tab==index,onClick={tab=index},
                        icon={Icon(icon,contentDescription=null)},
                        label={Text(name)})
                }
            }
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)
            .background(Brush.verticalGradient(listOf(navy,Color(0xFF0B1F35))))) {
            if(!linked) {
                Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
                    verticalArrangement=Arrangement.spacedBy(18.dp)) {
                    Icon(Icons.Default.Campaign,null,tint=crimson,modifier=Modifier.size(54.dp))
                    Text("Bereit, wenn es zählt.",style=MaterialTheme.typography.headlineLarge,
                        fontWeight=FontWeight.Black)
                    Text("Kopple dein Smartphone mit EinsatzPulse. Der Server bestimmt deine Einheiten und Alarmrechte.",
                        color=soft,style=MaterialTheme.typography.bodyLarge)
                    Card(colors=CardDefaults.cardColors(containerColor=panel),shape=r) {
                        Column(Modifier.padding(20.dp),verticalArrangement=Arrangement.spacedBy(14.dp)) {
                            OutlinedTextField(server,{server=it},label={Text("HTTPS-Server")},
                                placeholder={Text("https://einsatz.example.org")},singleLine=true,
                                modifier=Modifier.fillMaxWidth())
                            OutlinedTextField(code,{code=it},label={Text("Einmaliger Kopplungscode")},
                                singleLine=true,modifier=Modifier.fillMaxWidth())
                            OutlinedTextField(label,{label=it},label={Text("Gerätename")},
                                singleLine=true,modifier=Modifier.fillMaxWidth())
                            Button(onClick={
                                busy=true;error=""
                                scope.launch {
                                    try {
                                        api.activate(server,code,label)
                                        code="";linked=true;tab=0
                                        snackbar.showSnackbar("Gerät erfolgreich mit dem Server gekoppelt")
                                    } catch(e:Exception) {error=e.message?:"Kopplung fehlgeschlagen"}
                                    finally {busy=false}
                                }
                            },enabled=!busy && server.isNotBlank() && code.isNotBlank() && label.isNotBlank(),
                                modifier=Modifier.fillMaxWidth()) {
                                if(busy) CircularProgressIndicator(Modifier.size(17.dp),strokeWidth=2.dp)
                                else Icon(Icons.Default.Link,null)
                                Spacer(Modifier.width(8.dp));Text("Gerät verbinden")
                            }
                        }
                    }
                    if(error.isNotBlank()) Text(error,color=crimson)
                    Text("Den Code erzeugst du in EinsatzPulse → Meine Alarme → Android-Geräte.",
                        color=mint,style=MaterialTheme.typography.bodyMedium)
                    Text("Ergänzender Alarmkanal. Kein Ersatz für DME, Funk und Leitstelle.",
                        color=soft,style=MaterialTheme.typography.bodySmall)
                }
            } else {
                if(error.isNotBlank()) Surface(color=crimson.copy(alpha=.14f)) {
                    Text("Verbindung: $error",color=crimson,modifier=Modifier.fillMaxWidth().padding(12.dp))
                }
                when(tab) {
                    0 -> {
                        val all=dashboard?.incidents.orEmpty()
                            .filter { selectedUnit==0||it.unitId==selectedUnit }
                        val active=all.count { it.status=="active" }
                        LazyColumn(contentPadding=PaddingValues(18.dp),
                            verticalArrangement=Arrangement.spacedBy(13.dp)) {
                            item {
                                Column(verticalArrangement=Arrangement.spacedBy(8.dp)) {
                                    Text("Meine Einsätze",style=MaterialTheme.typography.headlineMedium,
                                        fontWeight=FontWeight.ExtraBold)
                                    Text("Direkt mit EinsatzPulse verbunden · $lastSync",color=soft)
                                    Row(horizontalArrangement=Arrangement.spacedBy(10.dp)) {
                                        StateCard("AKTIV","$active",Modifier.weight(1f),crimson)
                                        StateCard("RÜCKMELDUNGEN",
                                            "${all.count { it.reply.isNotBlank() }}",Modifier.weight(1f))
                                    }
                                    Text("Nur persönlich zugeordnete, geprüfte Einheiten",
                                        color=soft,style=MaterialTheme.typography.labelMedium)
                                }
                            }
                            if(dashboard?.units.orEmpty().size>1) item {
                                Row(horizontalArrangement=Arrangement.spacedBy(8.dp),
                                    modifier=Modifier.fillMaxWidth()) {
                                    FilterChip(selectedUnit==0,{selectedUnit=0},label={Text("Alle")})
                                    dashboard?.units?.take(3)?.forEach { u ->
                                        FilterChip(selectedUnit==u.id,{selectedUnit=u.id},
                                            label={Text(u.name,maxLines=1,overflow=TextOverflow.Ellipsis)})
                                    }
                                }
                            }
                            if(dashboard==null) item {
                                CircularProgressIndicator()
                            }
                            if(dashboard!=null && all.isEmpty()) item {
                                Card(colors=CardDefaults.cardColors(containerColor=panel),shape=r) {
                                    Column(Modifier.fillMaxWidth().padding(28.dp),
                                        horizontalAlignment=Alignment.CenterHorizontally) {
                                        Icon(Icons.Default.CheckCircle,null,tint=mint,modifier=Modifier.size(44.dp))
                                        Text("Keine zugeordneten Alarme",fontWeight=FontWeight.Bold)
                                        Text("Neue Einsätze erscheinen nach bestätigter Einheitenzuordnung.",
                                            color=soft)
                                    }
                                }
                            }
                            items(all,key={"${it.id}:${it.unitId}"}) { item ->
                                AlarmCard(item,busy=replyBusy.isNotBlank()) { status ->
                                    replyBusy="${item.id}:${item.unitId}"
                                    scope.launch {
                                        try {
                                            if(!api.reply(item.id,item.unitId,status))
                                                throw IllegalStateException("Server hat Rückmeldung nicht bestätigt")
                                            snackbar.showSnackbar("Rückmeldung vom Server bestätigt")
                                            dashboard=api.dashboard()
                                        } catch(e:Exception) {
                                            snackbar.showSnackbar("Nicht gespeichert: ${e.message}")
                                        } finally { replyBusy="" }
                                    }
                                }
                            }
                            item {
                                Text("Zusatzinformation, keine verbindliche BOS-Alarmierung. " +
                                    "Rückmeldungen sind erst nach Serverbestätigung gültig.",
                                    color=soft,style=MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                    1 -> {
                        LazyColumn(contentPadding=PaddingValues(20.dp),
                            verticalArrangement=Arrangement.spacedBy(14.dp)) {
                            item {Text("Meine Einheiten",style=MaterialTheme.typography.headlineMedium,
                                fontWeight=FontWeight.ExtraBold)}
                            items(dashboard?.units.orEmpty(),key={it.id}) { unit ->
                                Card(shape=r,colors=CardDefaults.cardColors(containerColor=panel)) {
                                    Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(10.dp)) {
                                        Text(unit.name,fontWeight=FontWeight.Bold,
                                            style=MaterialTheme.typography.titleMedium)
                                        Text(unit.organisation,color=soft)
                                        Text("Eigene Verfügbarkeit: ${unit.availability.ifBlank { "Nicht gemeldet" }}",
                                            color=mint)
                                        Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                                            listOf("available" to "Bereit","limited" to "Bedingt",
                                                "unavailable" to "Abwesend").forEach { (value,title) ->
                                                OutlinedButton(onClick={
                                                    scope.launch {
                                                        try {
                                                            if(!api.availability(unit.id,value))error("Nicht bestätigt")
                                                            dashboard=api.dashboard()
                                                            snackbar.showSnackbar("Verfügbarkeit bestätigt")
                                                        }catch(e:Exception){
                                                            snackbar.showSnackbar("Nicht gespeichert: ${e.message}")
                                                        }
                                                    }
                                                },contentPadding=PaddingValues(horizontal=8.dp,vertical=4.dp)) {
                                                    Text(title,style=MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    else -> {
                        val config=device
                        Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                            verticalArrangement=Arrangement.spacedBy(14.dp)) {
                            Text("Mein Alarmgerät",style=MaterialTheme.typography.headlineMedium,
                                fontWeight=FontWeight.ExtraBold)
                            if(config!=null) {
                                Card(colors=CardDefaults.cardColors(containerColor=panel),shape=r) {
                                    Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                        Text(config.label,fontWeight=FontWeight.Bold,
                                            style=MaterialTheme.typography.titleLarge)
                                        Row {
                                            Text(if(config.enabled)"● Alarmierung eingeschaltet"
                                                else "● Vom Server deaktiviert",
                                                color=if(config.enabled)mint else crimson)
                                        }
                                        Text("Ton: ${config.tone} · Vibration: ${if(config.vibrate)"Ja" else "Nein"}",
                                            color=soft)
                                        Text(if(config.pushRegistered)"FCM registriert"
                                            else "FCM noch nicht registriert – Firebase-Konfiguration prüfen",
                                            color=if(config.pushRegistered)mint else crimson)
                                        OutlinedButton(onClick={AlarmNotifications.preview(context,config.tone)}) {
                                            Icon(Icons.Default.VolumeUp,null)
                                            Spacer(Modifier.width(6.dp))
                                            Text("Alarmton ausprobieren")
                                        }
                                    }
                                }
                            }
                            Card(colors=CardDefaults.cardColors(containerColor=panel),shape=r) {
                                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                    Text("Android-Berechtigungen",fontWeight=FontWeight.Bold,
                                        style=MaterialTheme.typography.titleLarge)
                                    val notified=remember(diagnosticRefresh) {
                                        AlarmNotifications.notificationsEnabled(context)
                                    }
                                    val dnd=remember(diagnosticRefresh) {
                                        AlarmNotifications.dndGranted(context)
                                    }
                                    val battery=remember(diagnosticRefresh) {
                                        AlarmNotifications.batteryNotRestricted(context)
                                    }
                                    val priority=remember(diagnosticRefresh,config) {
                                        config!=null && AlarmNotifications.priorityChannelPrepared(context,config)
                                    }
                                    Text("Benachrichtigungen: "+if(notified)"Freigegeben" else "Blockiert",
                                        color=if(notified)mint else crimson)
                                    Text("Nicht-stören-Zugriff: "+if(dnd)"Vom System erteilt" else "Nicht erteilt",
                                        color=if(dnd)mint else soft)
                                    Text("Akku-Ausnahme: "+if(battery)"Vorhanden" else "Nicht eingerichtet",
                                        color=if(battery)mint else soft)
                                    Text("Prioritätskanal: "+if(priority)"Konfiguriert" else "Nicht vorbereitet",
                                        color=if(priority)mint else soft)
                                    Text("Android kann Prioritätsalarme nur mit deiner Einwilligung zulassen. " +
                                        "Die Servereinstellung alleine schaltet Nicht stören nicht aus.",
                                        color=soft,style=MaterialTheme.typography.bodySmall)
                                    Button(onClick={
                                        context.startActivity(Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    }) {
                                        Icon(Icons.Default.NotificationsActive,null)
                                        Spacer(Modifier.width(6.dp))
                                        Text("Nicht-stören-Zugriff prüfen")
                                    }
                                    if(Build.VERSION.SDK_INT>=33) OutlinedButton(onClick={
                                        notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
                                    }) {Text("Benachrichtigungen erlauben")}
                                    OutlinedButton(onClick={
                                        context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                            .putExtra(Settings.EXTRA_APP_PACKAGE,context.packageName)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    }) {Text("Android-Benachrichtigungskanäle öffnen")}
                                    if(config!=null) OutlinedButton(onClick={
                                        val success=runCatching {
                                            AlarmNotifications.prepareChannel(context,config)
                                        }.getOrDefault(false)
                                        diagnosticRefresh++
                                        scope.launch { snackbar.showSnackbar(
                                            if(success)"Alarmkanal vorbereitet. Android-Übersteuerung bleibt systemabhängig."
                                            else "Alarmkanal nicht bereit: Berechtigungen und Servereinstellungen prüfen."
                                        ) }
                                    }) {Text("Alarmkanal vorbereiten und prüfen")}
                                    OutlinedButton(onClick={
                                        context.startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                                    }) {Text("Akku-Optimierung prüfen")}
                                    TextButton(onClick={
                                        diagnosticRefresh++
                                        refresh()
                                    }) {Text("Gerätestatus aktualisieren")}
                                }
                            }
                            Card(colors=CardDefaults.cardColors(containerColor=panel),shape=r) {
                                Column(Modifier.padding(18.dp),verticalArrangement=Arrangement.spacedBy(12.dp)) {
                                    Text("Verbindung",fontWeight=FontWeight.Bold)
                                    Text(session.server,color=soft)
                                    Text("Geräteeinstellungen und Widerruf: EinsatzPulse → Meine Alarme → Android-Geräte",
                                        color=mint)
                                    Button(onClick={
                                        busy=true;error=""
                                        scope.launch {
                                            try {
                                                if(!api.unlink()) error("Serverwiderruf nicht bestätigt")
                                                else {
                                                    AlarmNotifications.dismissAll(context)
                                                    session.clear()
                                                    linked=false;dashboard=null;device=null
                                                    snackbar.showSnackbar("Gerätezugang serverseitig widerrufen")
                                                }
                                            } catch(e:Exception) {
                                                error="Widerruf nicht bestätigt: ${e.message}. Alternativ im Web widerrufen."
                                            } finally {busy=false}
                                        }
                                    },enabled=!busy) {Text("Gerät am Server widerrufen")}
                                    Text("Lokales Abmelden widerruft den Serverzugang NICHT. "+
                                        "Bei fehlender Verbindung anschließend im Web das Gerät löschen.",
                                        color=soft,style=MaterialTheme.typography.bodySmall)
                                    OutlinedButton(onClick={
                                        AlarmNotifications.dismissAll(context)
                                        session.clear()
                                        linked=false;dashboard=null;device=null;error=""
                                        snackbar.currentSnackbarData?.dismiss()
                                    },enabled=!busy) {Text("Nur lokal abmelden")}
                                }
                            }
                            Text("FCM ist bestmöglich, nicht garantiert. Energiesparen, Android-Sperren, " +
                                "Provider und Netz können die Alarmierung verzögern oder verhindern.",
                                color=soft,style=MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }
    }
}
