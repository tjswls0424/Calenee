package org.jin.calenee.home

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.databinding.DataBindingUtil
import org.jin.calenee.R
import org.jin.calenee.databinding.ActivityEditCoupleInfoBinding

class EditCoupleInfoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditCoupleInfoBinding

    private lateinit var coupleInfo: CoupleInfo
    private var position: Int = 0

    companion object {
        var enableFlag1 = false
        var enableFlag2 = false
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_edit_couple_info)

        initData()
        initToolbar()
        listener()
    }

    private fun initData() {
        intent.apply {
            coupleInfo = getSerializableExtra("coupleInfo") as CoupleInfo
            position = getIntExtra("position", 0)
        }

        binding.companion = EditCoupleInfoActivity.Companion
        binding.coupleInfo = coupleInfo
        makeViewEditable()
    }

    private fun makeViewEditable() = when (position) {
        1 -> enableFlag1 = true
        2 -> enableFlag2 = true

        else -> {
            enableFlag1 = false
            enableFlag2 = false
        }
    }

    private fun listener() {
        binding.firstMetDateRow.setOnClickListener {
            Log.d("row_test", "firstMetDateRow")
        }

        if (enableFlag1) {
            binding.user1Row.setOnClickListener {
                Log.d("row_test", "user1Row")
            }
            binding.user1BirthdayRow.setOnClickListener {
                Log.d("row_test", "user1BirthdayRow")
            }
            binding.user1MessageRow.setOnClickListener {
                Log.d("row_test", "user1MessageRow")
            }
        }

        if (enableFlag2) {
            binding.user2Row.setOnClickListener {
                Log.d("row_test", "user2Row")
            }
            binding.user2BirthdayRow.setOnClickListener {
                Log.d("row_test", "user2BirthdayRow")
            }
            binding.user2MessageRow.setOnClickListener {
                Log.d("row_test", "user2MessageRow")
            }

        }
    }

    private fun initToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowTitleEnabled(false)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> onBackPressed()
        }

        return super.onOptionsItemSelected(item)
    }
}