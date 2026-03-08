package com.kishor.gpstools

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.DocumentsContract
import android.provider.Settings
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.chip.ChipGroup
import com.google.android.material.textview.MaterialTextView
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: MetadataViewModel
    private lateinit var sourceFoldersText: MaterialTextView
    private lateinit var destinationFolderText: MaterialTextView
    private lateinit var actionChipGroup: ChipGroup
    private lateinit var timeWindowInput: EditText
    private lateinit var statusText: MaterialTextView
    private lateinit var progressText: MaterialTextView
    private lateinit var totalFilesText: MaterialTextView
    private lateinit var movedFilesText: MaterialTextView
    private lateinit var keptFilesText: MaterialTextView
    private lateinit var taggedFilesText: MaterialTextView
    private lateinit var includeSubfoldersCheckbox: com.google.android.material.checkbox.MaterialCheckBox
    
    private val sourceFolders = mutableListOf<String>()
    private var destinationFolder: String? = null
    
    private val selectSourceFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            handleDirectorySelection(it, isSource = true)
        }
    }
    
    private val selectDestinationFolderLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri ->
        uri?.let {
            handleDirectorySelection(it, isSource = false)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        viewModel = ViewModelProvider(this)[MetadataViewModel::class.java]
        
        initializeViews()
        setupClickListeners()
        observeViewModel()
        checkPermissions()
    }
    
    private fun handleDirectorySelection(uri: Uri, isSource: Boolean) {
        try {
            contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            
            val path = getPathFromTreeUri(uri)
            
            if (path != null) {
                if (isSource) {
                    if (!sourceFolders.contains(path)) {
                        sourceFolders.add(path)
                        updateSourceFoldersDisplay()
                        Toast.makeText(this, "Added: $path", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this, "Folder already added", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    destinationFolder = path
                    updateDestinationFolderDisplay()
                    Toast.makeText(this, "Selected: $path", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Could not access folder. Try selecting a different folder.", Toast.LENGTH_LONG).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun getPathFromTreeUri(treeUri: Uri): String? {
        return try {
            val docId = DocumentsContract.getTreeDocumentId(treeUri)
            val split = docId.split(":")
            
            when {
                docId.startsWith("primary:") -> {
                    val path = split.getOrNull(1) ?: ""
                    if (path.isEmpty()) {
                        Environment.getExternalStorageDirectory().absolutePath
                    } else {
                        "${Environment.getExternalStorageDirectory().absolutePath}/$path"
                    }
                }
                split.size >= 2 -> {
                    val storageId = split[0]
                    val path = split[1]
                    "/storage/$storageId/$path"
                }
                else -> {
                    Environment.getExternalStorageDirectory().absolutePath
                }
            }
        } catch (e: Exception) {
            null
        }
    }
    
    private fun initializeViews() {
        sourceFoldersText = findViewById(R.id.sourceFoldersText)
        destinationFolderText = findViewById(R.id.destinationFolderText)
        actionChipGroup = findViewById(R.id.actionChipGroup)
        timeWindowInput = findViewById(R.id.timeWindowInput)
        statusText = findViewById(R.id.statusText)
        progressText = findViewById(R.id.progressText)
        totalFilesText = findViewById(R.id.totalFilesText)
        movedFilesText = findViewById(R.id.movedFilesText)
        keptFilesText = findViewById(R.id.keptFilesText)
        taggedFilesText = findViewById(R.id.taggedFilesText)
        includeSubfoldersCheckbox = findViewById(R.id.includeSubfoldersCheckbox)
    }
    
    private fun setupClickListeners() {
        findViewById<MaterialButton>(R.id.addSourceButton).setOnClickListener {
            selectSourceFolderLauncher.launch(null)
        }
        
        findViewById<MaterialButton>(R.id.clearSourceButton).setOnClickListener {
            sourceFolders.clear()
            updateSourceFoldersDisplay()
            resetStatistics()
            Toast.makeText(this, "All folders cleared", Toast.LENGTH_SHORT).show()
        }
        
        findViewById<MaterialButton>(R.id.selectDestinationButton).setOnClickListener {
            selectDestinationFolderLauncher.launch(null)
        }
        
        findViewById<MaterialButton>(R.id.buildDatabaseButton).setOnClickListener {
            startDatabaseBuild()
        }
        
        findViewById<MaterialButton>(R.id.startAutoTagButton).setOnClickListener {
            startAutoTag()
        }
    }
    
    private fun observeViewModel() {
        lifecycleScope.launch {
            viewModel.status.collect { status ->
                statusText.text = status
            }
        }
        
        lifecycleScope.launch {
            viewModel.progress.collect { progress ->
                progressText.text = progress
            }
        }
        
        lifecycleScope.launch {
            viewModel.statistics.collect { stats ->
                totalFilesText.text = stats.total.toString()
                movedFilesText.text = stats.moved.toString()
                keptFilesText.text = stats.kept.toString()
                taggedFilesText.text = stats.tagged.toString()
            }
        }
    }
    
    private fun checkPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                AlertDialog.Builder(this)
                    .setTitle("Storage Permission Required")
                    .setMessage("This app needs access to manage files. Please grant 'All files access' permission.")
                    .setPositiveButton("Grant Permission") { _, _ ->
                        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
                        intent.data = Uri.parse("package:$packageName")
                        startActivity(intent)
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            
            val permissionsToRequest = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            
            if (permissionsToRequest.isNotEmpty()) {
                ActivityCompat.requestPermissions(
                    this,
                    permissionsToRequest.toTypedArray(),
                    PERMISSION_REQUEST_CODE
                )
            }
        }
    }
    
    private fun updateSourceFoldersDisplay() {
        if (sourceFolders.isEmpty()) {
            sourceFoldersText.text = "No folders selected"
        } else {
            sourceFoldersText.text = sourceFolders.joinToString("\n") { "🗂️ $it" }
        }
    }
    
    private fun updateDestinationFolderDisplay() {
        destinationFolderText.text = if (destinationFolder != null) {
            "🗂️ $destinationFolder"
        } else {
            "No folder selected"
        }
    }
    
    private fun resetStatistics() {
        totalFilesText.text = "0"
        movedFilesText.text = "0"
        keptFilesText.text = "0"
        taggedFilesText.text = "0"
    }
    
    private fun startDatabaseBuild() {
        if (sourceFolders.isEmpty()) {
            Toast.makeText(this, "Please select source folders", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (destinationFolder == null) {
            Toast.makeText(this, "Please select destination folder", Toast.LENGTH_SHORT).show()
            return
        }
        
        val timeWindow = timeWindowInput.text.toString().toIntOrNull()
        if (timeWindow == null || timeWindow <= 0) {
            Toast.makeText(this, "Please enter valid time window (minutes)", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewModel.setTimeWindow(timeWindow)
        
        val selectedAction = when (actionChipGroup.checkedChipId) {
            R.id.chipMove -> FileAction.MOVE
            R.id.chipCopy -> FileAction.COPY
            else -> FileAction.NOTHING
        }
        
        val includeSubfolders = includeSubfoldersCheckbox.isChecked
        
        resetStatistics()
        
        lifecycleScope.launch {
            viewModel.buildDatabase(
                sourceFolders = sourceFolders,
                destinationFolder = destinationFolder!!,
                fileAction = selectedAction,
                includeSubfolders = includeSubfolders
            )
            
            showAutoTagConfirmationDialog()
        }
    }
    
    private fun showAutoTagConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Database Created")
            .setMessage("GPS database created successfully.\nStart AutoTag process?")
            .setPositiveButton("YES") { _, _ ->
                startAutoTag()
            }
            .setNegativeButton("NO", null)
            .show()
    }
    
    private fun startAutoTag() {
        if (destinationFolder == null) {
            Toast.makeText(this, "Please build database first", Toast.LENGTH_SHORT).show()
            return
        }
        
        val timeWindow = timeWindowInput.text.toString().toIntOrNull()
        if (timeWindow == null || timeWindow <= 0) {
            Toast.makeText(this, "Please enter valid time window", Toast.LENGTH_SHORT).show()
            return
        }
        
        viewModel.setTimeWindow(timeWindow)
        
        lifecycleScope.launch {
            viewModel.startAutoTag(destinationFolder!!)
            Toast.makeText(this@MainActivity, "AutoTag completed", Toast.LENGTH_SHORT).show()
        }
    }
    
    companion object {
        private const val PERMISSION_REQUEST_CODE = 100
    }
}
