package de.einsatzpulse.alarm

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

private val ink=Color(0xFF0A1426)
private val surface=Color(0xFF17263D)
private val accent=Color(0xFF6EDCC5)
private val danger=Color(0xFFFF5962)
private val pale=Color(0xFFB7C4D8)

private fun heading(kind:String)=when(kind) {
    "hall" -> "HALLENMONITOR"
    "vehicle" -> "FAHRZEUGMONITOR"
    else -> "EINSATZKRAFT-ANSICHT"
}

/** Native kiosk surface. Only server-scoped incidents; never fixture actions. */
@Composable
fun NativeMonitorScene(runtime:AlarmRuntime,lastSync:String,connectionError:String,onSettings:()->Unit) {
    val demo=runtime.testView
    val incidents=runtime.incidents
    val active=incidents.isNotEmpty() && !demo
    val vehicle=runtime.vehicle
    LazyColumn(
        modifier=Modifier.fillMaxSize().background(ink),
        contentPadding=PaddingValues(horizontal=20.dp,vertical=16.dp),
        verticalArrangement=Arrangement.spacedBy(16.dp),
    ) {
        item {
            Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.SpaceBetween,
                verticalAlignment=Alignment.CenterVertically) {
                Column {
                    Text(heading(runtime.displayKind),color=accent,
                        fontWeight=FontWeight.ExtraBold,
                        style=MaterialTheme.typography.labelLarge)
                    Text(if(runtime.displayKind=="vehicle")
                            vehicle?.callsign ?: "Fahrzeug nicht zugeordnet"
                         else runtime.units.joinToString(" · ") { it.name }.ifBlank {"Einheiten"},
                        color=Color.White,fontWeight=FontWeight.Black,
                        style=MaterialTheme.typography.headlineMedium,
                        maxLines=2,overflow=TextOverflow.Ellipsis)
                }
                IconButton(onClick=onSettings) {
                    Icon(Icons.Default.Settings,"Geräteinformationen",tint=pale)
                }
            }
        }
        if(connectionError.isNotBlank()) item {
            Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF87292F)),
                shape=RoundedCornerShape(14.dp)) {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    Text("VERBINDUNG UNTERBROCHEN – ANZEIGE KANN VERALTET SEIN",
                        color=Color.White,fontWeight=FontWeight.Black,
                        style=MaterialTheme.typography.titleMedium)
                    Text("Letzter erfolgreicher Datenabruf: "+lastSync,
                        color=Color.White,style=MaterialTheme.typography.bodySmall)
                    Text("Keine sichere Aussage über aktuelle Einsätze. BOS-Alarmwege prüfen.",
                        color=Color.White,style=MaterialTheme.typography.bodySmall)
                }
            }
        }
        if(demo) item {
            Card(colors=CardDefaults.cardColors(containerColor=Color(0xFF69420E)),
                shape=RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(20.dp)) {
                    Text("TESTANSICHT – KEIN ECHTER EINSATZ",color=Color.White,
                        fontWeight=FontWeight.Black,
                        style=MaterialTheme.typography.titleLarge)
                    Text("Vom Administrator zeitlich begrenzt aktiviert. "+
                        "Keine Rückmeldungen, Fahrzeugstatus oder GPS-Übertragung.",
                        color=Color.White,style=MaterialTheme.typography.bodyMedium)
                }
            }
        }
        item {
            Card(shape=RoundedCornerShape(20.dp),
                colors=CardDefaults.cardColors(containerColor=
                    if(active) danger.copy(alpha=0.23f) else surface)) {
                Row(Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment=Alignment.CenterVertically,
                    horizontalArrangement=Arrangement.spacedBy(15.dp)) {
                    Icon(if(active)Icons.Default.Warning else Icons.Default.CheckCircle,
                        contentDescription=null,
                        tint=if(active)danger else accent,modifier=Modifier.size(42.dp))
                    Column {
                        Text(when {
                            demo -> "VORFÜHRUNG / SIMULATION"
                            active -> "AKTIVER EINSATZ"
                            else -> "KEIN AKTIVER EINSATZ"
                        },color=if(active)danger else accent,
                            style=MaterialTheme.typography.titleLarge,
                            fontWeight=FontWeight.Black)
                        Text(when {
                            demo -> "Fiktive Ansicht – ausschließlich zu Testzwecken"
                            active -> "${incidents.size} zugeordnete Einsatzmeldung(en)"
                            else -> "Bereitschaft · Live-Verbindung zum EinsatzPulse-Server"
                        },color=pale,style=MaterialTheme.typography.bodyMedium)
                    }
                }
            }
        }
        if(runtime.displayKind=="vehicle" && vehicle!=null) item {
            Card(colors=CardDefaults.cardColors(containerColor=surface),
                shape=RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(18.dp)) {
                    Text("FAHRZEUG",color=accent,fontWeight=FontWeight.Bold)
                    Text(vehicle.name,color=Color.White,
                        style=MaterialTheme.typography.titleLarge,fontWeight=FontWeight.Bold)
                    Text(vehicle.callsign,color=pale)
                    Text("Keine automatische Übermittlung eines BOS-Funkstatus",
                        color=pale,style=MaterialTheme.typography.labelSmall)
                }
            }
        }
        items(incidents,key={ "${it.id}:${it.unitId}:${it.demo}" }) { item ->
            Card(shape=RoundedCornerShape(20.dp),
                colors=CardDefaults.cardColors(containerColor=surface)) {
                Column(Modifier.fillMaxWidth().padding(20.dp),
                    verticalArrangement=Arrangement.spacedBy(10.dp)) {
                    GroupTitle(if(item.demo)"FIKTIVER DEMOEINSATZ" else item.unitName)
                    Text(item.keyword.ifBlank {"Einsatzmeldung"},
                        color=Color.White,style=MaterialTheme.typography.headlineMedium,
                        fontWeight=FontWeight.ExtraBold)
                    Row(horizontalArrangement=Arrangement.spacedBy(8.dp)) {
                        Text("#"+item.number,color=pale,fontWeight=FontWeight.Medium)
                        Text(if(item.demo)"SIMULIERT" else item.status.uppercase(),
                            color=if(item.demo)Color(0xFFFFCD81) else danger,
                            fontWeight=FontWeight.Bold)
                    }
                    if(item.town.isNotBlank()||item.street.isNotBlank()) {
                        Row(verticalAlignment=Alignment.CenterVertically) {
                            Icon(Icons.Default.LocationOn,null,tint=accent)
                            Spacer(Modifier.width(6.dp))
                            Text(listOf(item.street,item.town).filter { it.isNotBlank() }
                                .joinToString(" · "),color=Color.White,
                                style=MaterialTheme.typography.titleMedium)
                        }
                    }
                    if(runtime.kind=="person" && !demo)
                        Text("Einsatzkraft-Rückmeldungen erfolgen ausschließlich im persönlichen Bereich.",
                            color=pale,style=MaterialTheme.typography.labelSmall)
                }
            }
        }
        item {
            Text("Serverabfrage: $lastSync · Live nur bei geöffneter App und Internet. "+
                "Kein garantierter Hintergrund-Push und kein Ersatz für BOS-Alarmmittel.",
                color=pale,style=MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun GroupTitle(name:String){
    Text(name,color=accent,style=MaterialTheme.typography.labelLarge,
        fontWeight=FontWeight.Bold)
}
