package com.maary.shareas

import android.os.Build
import android.os.Bundle
import androidx.activity.addCallback
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.color.DynamicColors
import com.maary.shareas.databinding.ActivityWelcomeBinding
import com.maary.shareas.fragment.WelcomeFinishFragment
import com.maary.shareas.fragment.WelcomeHistoryFragment
import com.maary.shareas.fragment.WelcomeSystemFragment

private const val NUM_PAGES = 3
class WelcomeActivity : FragmentActivity() {

    private lateinit var viewPager: ViewPager2
    private lateinit var binding: ActivityWelcomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        DynamicColors.applyToActivityIfAvailable(this)

        binding = DataBindingUtil.setContentView(this, R.layout.activity_welcome)

        viewPager = binding.pager
        val pagerAdapter = WelcomeFragmentAdapter(this)
        viewPager.adapter = pagerAdapter
        viewPager.isUserInputEnabled = false

        onBackPressedDispatcher.addCallback {
            if (viewPager.currentItem == 0) {
                // If the user is currently looking at the first step, allow the system to handle
                // the Back button. This calls finish() on this activity and pops the back stack.
                finish()
            } else {
                // Otherwise, select the previous step.
                viewPager.currentItem -= 1
            }
        }

    }




    private inner class WelcomeFragmentAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = NUM_PAGES

        override fun createFragment(position: Int): Fragment {
            return when (position) {
                0 -> WelcomeHistoryFragment()
                1 -> WelcomeSystemFragment()
                else -> WelcomeFinishFragment()
            }
        }
    }

}