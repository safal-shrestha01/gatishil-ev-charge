package com.gatishil.evcharge

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Color(0xFF006400))) {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "dashboard") {
        composable("dashboard") { DashboardScreen(navController) }
        composable("fast_charge") { FastChargeScreen(navController) }
        composable("history") { HistoryScreen(navController) }
        composable("daily_summary") { DailySummaryScreen(navController) }
        composable("settings") { SettingsScreen(navController) }
    }
}

// --- VIEWMODEL ---
class AppViewModel(private val db: AppDatabase) : ViewModel() {
    private val txDao = db.transactionDao()
    private val setDao = db.settingsDao()

    suspend fun saveTransaction(tx: Transaction) = txDao.insert(tx)
    suspend fun getTransactions() = txDao.getAll()
    suspend fun getTransactionsByDate(start: Long, end: Long) = txDao.getByDateRange(start, end)
    suspend fun searchTransactions(query: String) = txDao.search(query)
    suspend fun getSettings() = setDao.getSettings() ?: AppSettings()
    suspend fun saveSettings(settings: AppSettings) = setDao.saveSettings(settings)
}

val viewModelFactory = object : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppViewModel(MainApp.db) as T
    }
}

object MainApp {
    lateinit var db: AppDatabase
}

// --- SCREENS ---

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    MainApp.db = AppDatabase.getDatabase(context)

    Scaffold(
        topBar = { TopAppBar(title = { Text("Gatishil Yatayat EV") }) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(onClick = { navController.navigate("fast_charge") }, modifier = Modifier.fillMaxWidth().height(100.dp)) {
                Text("FAST CHARGE", fontSize = 24.sp)
            }
            Button(onClick = { Toast.makeText(context, "Coming Soon", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth().height(100.dp), enabled = false) {
                Text("CAR WASH (Coming Soon)", fontSize = 20.sp)
            }
            Button(onClick = { Toast.makeText(context, "Coming Soon", Toast.LENGTH_SHORT).show() }, modifier = Modifier.fillMaxWidth().height(100.dp), enabled = false) {
                Text("SLOW CHARGE (Coming Soon)", fontSize = 20.sp)
            }
            
            Spacer(modifier = Modifier.height(32.dp))
            OutlinedButton(onClick = { navController.navigate("history") }, modifier = Modifier.fillMaxWidth()) { Text("Transaction History") }
            OutlinedButton(onClick = { navController.navigate("daily_summary") }, modifier = Modifier.fillMaxWidth()) { Text("Daily Summary") }
            OutlinedButton(onClick = { navController.navigate("settings") }, modifier = Modifier.fillMaxWidth()) { Text("Settings") }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FastChargeScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    val vm: AppViewModel = viewModel(factory = viewModelFactory)
    val scope = rememberCoroutineScope()

    var model by remember { mutableStateOf("") }
    var vehicleNo by remember { mutableStateOf("") }
    var startBat by remember { mutableStateOf("") }
    var endBat by remember { mutableStateOf("") }
    var paymentMode by remember { mutableStateOf("Cash") }
    var rate by remember { mutableStateOf(25) }
    var isSaving by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { rate = vm.getSettings().ratePerPercent }

    val startInt = startBat.toIntOrNull() ?: 0
    val endInt = endBat.toIntOrNull() ?: 0
    val charged = (endInt - startInt).coerceAtLeast(0)
    val bill = charged * rate

    Scaffold(topBar = { TopAppBar(title = { Text("Fast Charge") }, navigationIcon = { TextButton(onClick = { navController.popBackStack() }) { Text("Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(value = model, onValueChange = { model = it }, label = { Text("Vehicle Model") }, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(value = vehicleNo, onValueChange = { vehicleNo = it }, label = { Text("Vehicle No.") }, modifier = Modifier.fillMaxWidth())
            
            Row(Modifier.fillMaxWidth()) {
                OutlinedTextField(value = startBat, onValueChange = { startBat = it.filter { c -> c.isDigit() } }, label = { Text("Start %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
                Spacer(Modifier.width(8.dp))
                OutlinedTextField(value = endBat, onValueChange = { endBat = it.filter { c -> c.isDigit() } }, label = { Text("End %") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.weight(1f))
            }

            Card(Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
                Column(Modifier.padding(16.dp)) {
                    Text("Charged: $charged %", fontSize = 20.sp, fontWeight = FontWeight.Bold)
                    Text("Bill Amount: NPR $bill", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
            }

            Text("Mode of Payment:")
            Row {
                listOf("Cash", "Bank", "Credit").forEach { mode ->
                    FilterChip(selected = paymentMode == mode, onClick = { paymentMode = mode }, label = { Text(mode) }, modifier = Modifier.padding(end = 8.dp))
                }
            }

            Spacer(Modifier.weight(1f))

            Button(
                onClick = {
                    if (model.isBlank() || vehicleNo.isBlank() || startBat.isBlank() || endBat.isBlank()) {
                        Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (startInt < 0 || startInt > 100 || endInt < 0 || endInt > 100) {
                        Toast.makeText(context, "Battery % must be 0-100", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (endInt < startInt) {
                        Toast.makeText(context, "End % cannot be less than Start %", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (charged == 0) {
                        Toast.makeText(context, "Charged % cannot be zero", Toast.LENGTH_SHORT).show()
                        return@Button
                    }
                    if (isSaving) return@Button
                    isSaving = true

                    scope.launch {
                        val tx = Transaction(
                            txNumber = "TX${System.currentTimeMillis()}",
                            timestamp = System.currentTimeMillis(),
                            vehicleModel = model, vehicleNo = vehicleNo,
                            startBattery = startInt, endBattery = endInt,
                            chargedBattery = charged, rateUsed = rate,
                            billAmount = bill, paymentMode = paymentMode
                        )
                        vm.saveTransaction(tx)
                        val file = PdfGenerator.generateReceipt(context, tx)
                        Toast.makeText(context, "Transaction Saved!", Toast.LENGTH_LONG).show()
                        PdfGenerator.sharePdf(context, file)
                        isSaving = false
                        navController.popBackStack()
                    }
                },
                modifier = Modifier.fillMaxWidth().height(60.dp),
                enabled = !isSaving
            ) {
                Text(if (isSaving) "Saving..." else "SAVE TRANSACTION", fontSize = 20.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    val vm: AppViewModel = viewModel(factory = viewModelFactory)
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    var searchQuery by remember { mutableStateOf("") }

    LaunchedEffect(searchQuery) {
        transactions = if (searchQuery.isBlank()) vm.getTransactions() else vm.searchTransactions(searchQuery)
    }

    Scaffold(topBar = { TopAppBar(title = { Text("History") }, navigationIcon = { TextButton(onClick = { navController.popBackStack() }) { Text("Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(value = searchQuery, onValueChange = { searchQuery = it }, label = { Text("Search Vehicle/TX No") }, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(8.dp))
            
            val totalBill = transactions.sumOf { it.billAmount }
            Text("Total Transactions: ${transactions.size} | Total Billed: NPR $totalBill", fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))

            LazyColumn {
                items(transactions) { tx ->
                    val date = SimpleDateFormat("dd/MM/yy HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                    Card(Modifier.fillMaxWidth().padding(vertical = 4.dp), onClick = {
                        val file = PdfGenerator.generateReceipt(context, tx)
                        PdfGenerator.sharePdf(context, file)
                    }) {
                        Column(Modifier.padding(12.dp)) {
                            Text("${tx.txNumber} | ${tx.vehicleNo}", fontWeight = FontWeight.Bold)
                            Text("$date | ${tx.chargedBattery}% | NPR ${tx.billAmount} | ${tx.paymentMode}")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DailySummaryScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    val vm: AppViewModel = viewModel(factory = viewModelFactory)
    var transactions by remember { mutableStateOf<List<Transaction>>(emptyList()) }
    val calendar = Calendar.getInstance()
    var selectedDateStr by remember { mutableStateOf(SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(calendar.time)) }

    LaunchedEffect(selectedDateStr) {
        val start = calendar.apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0) }.timeInMillis
        val end = start + 86400000
        transactions = vm.getTransactionsByDate(start, end)
    }

    val cash = transactions.filter { it.paymentMode == "Cash" }.sumOf { it.billAmount }
    val bank = transactions.filter { it.paymentMode == "Bank" }.sumOf { it.billAmount }
    val credit = transactions.filter { it.paymentMode == "Credit" }.sumOf { it.billAmount }

    Scaffold(topBar = { TopAppBar(title = { Text("Daily Summary") }, navigationIcon = { TextButton(onClick = { navController.popBackStack() }) { Text("Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            Text("Date: $selectedDateStr", fontSize = 18.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(16.dp))
            
            LazyColumn(Modifier.weight(1f)) {
                item {
                    Row(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.primaryContainer).padding(8.dp)) {
                        listOf("S.N.", "Time", "Vehicle", "Chg%", "Amt", "Mode").forEach { Text(it, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f), fontSize = 12.sp) }
                    }
                }
                items(transactions) { tx ->
                    val time = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(tx.timestamp))
                    Row(Modifier.fillMaxWidth().padding(4.dp)) {
                        Text("${transactions.indexOf(tx) + 1}", modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Text(time, modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Text(tx.vehicleNo, modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Text("${tx.chargedBattery}%", modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Text("${tx.billAmount}", modifier = Modifier.weight(1f), fontSize = 12.sp)
                        Text(tx.paymentMode, modifier = Modifier.weight(1f), fontSize = 12.sp)
                    }
                }
            }

            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Text("Total TX: ${transactions.size} | Total Charged: ${transactions.sumOf { it.chargedBattery }}%")
                    Text("Total Bill: NPR ${transactions.sumOf { it.billAmount }}", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text("Cash: NPR $cash | Bank: NPR $bank | Credit: NPR $credit")
                }
            }
            
            Spacer(Modifier.height(8.dp))
            Row {
                Button(onClick = {
                    val file = PdfGenerator.exportCsv(context, transactions, selectedDateStr.replace(" ", "_"))
                    Toast.makeText(context, "CSV Saved to Documents", Toast.LENGTH_LONG).show()
                }, modifier = Modifier.weight(1f)) { Text("Export CSV") }
                Spacer(Modifier.width(8.dp))
                Button(onClick = { Toast.makeText(context, "Use Android Print Dialog for PDF", Toast.LENGTH_LONG).show() }, modifier = Modifier.weight(1f)) { Text("Print / PDF") }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(navController: androidx.navigation.NavController) {
    val context = LocalContext.current
    val vm: AppViewModel = viewModel(factory = viewModelFactory)
    var rate by remember { mutableStateOf("") }

    LaunchedEffect(Unit) { rate = vm.getSettings().ratePerPercent.toString() }

    Scaffold(topBar = { TopAppBar(title = { Text("Settings") }, navigationIcon = { TextButton(onClick = { navController.popBackStack() }) { Text("Back") } }) }) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            OutlinedTextField(value = rate, onValueChange = { rate = it.filter { c -> c.isDigit() } }, label = { Text("Rate per Percentage Point (NPR)") }, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            
            Spacer(Modifier.height(16.dp))
            Button(onClick = {
                val intRate = rate.toIntOrNull() ?: 25
                vm.viewModelScope.launch {
                    vm.saveSettings(AppSettings(ratePerPercent = intRate))
                    Toast.makeText(context, "Rate Saved", Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Save Rate") }

            Spacer(Modifier.height(32.dp))
            Text("Data Backup & Restore", fontWeight = FontWeight.Bold, fontSize = 18.sp)
            Text("Your data is stored locally on this device. If you uninstall the app or clear its data, transactions will be lost. Please use the Export/Import features regularly.", fontSize = 14.sp)
        }
    }
}
