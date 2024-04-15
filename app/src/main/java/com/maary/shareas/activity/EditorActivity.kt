package com.maary.shareas.activity

import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import com.maary.shareas.R
import com.maary.shareas.WallpaperViewModel
import com.maary.shareas.databinding.ActivityEditorBinding
import com.maary.shareas.helper.Util

class EditorActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditorBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.activity_editor)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.appBarContainer.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            binding.editorButtons.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val viewModel: WallpaperViewModel by viewModels()

//        val bitmap = Util.getBitmap(intent, this)
//        intent.data?.let { binding.editorView.setImageUri(it) }
        val bitmap: Bitmap? = null
//        bitmap?.let { binding.editorView.setImageBitmap(it) }
        if (bitmap!=null){
            binding.editorView.setImageBitmap(bitmap)
        }else{
            Log.v("EDA", "BITMAP NULL")
        }

        binding.appbarButtonCancel.setOnClickListener {
            finish()
        }

        binding.appbarButtonConfirm.setOnClickListener {
            //todo: save final edit
            finish()
        }

        binding.editorButtonSave.setOnClickListener {
            //todo: save current edit
            binding.editorCard.visibility = View.INVISIBLE
        }

        binding.editorButtonAbort.setOnClickListener {
            //todo: abort edit
            binding.editorCard.visibility = View.INVISIBLE
        }


        binding.editorButtonBlur.setOnClickListener {
            binding.editorStub.viewStub?.layoutResource = R.layout.layout_adjustment_slider
            val blurSlider = binding.editorStub.viewStub?.inflate()
            binding.editorCard.visibility = View.VISIBLE
        }

        binding.editorButtonBrightness.setOnClickListener {
            binding.editorStub.viewStub?.layoutResource = R.layout.layout_adjustment_slider
            val brightnessSlider = binding.editorStub.viewStub?.inflate()
            val title = brightnessSlider!!.findViewById<TextView>(R.id.adjustment_title)
            title.setText(R.string.brightness)
            binding.editorCard.visibility = View.VISIBLE
        }

    }
}