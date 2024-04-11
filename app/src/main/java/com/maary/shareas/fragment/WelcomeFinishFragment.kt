package com.maary.shareas.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.marginBottom
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.appbar.AppBarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.maary.shareas.helper.PreferencesHelper
import com.maary.shareas.R
import com.maary.shareas.activity.StartActivity
import com.maary.shareas.databinding.FragmentWelcomeFinishBinding
import kotlinx.coroutines.launch

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [WelcomeFinishFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class WelcomeFinishFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private var _binding: FragmentWelcomeFinishBinding? = null
    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentWelcomeFinishBinding.inflate(inflater, container, false)
        // Inflate the layout for this fragment
        val rootView = _binding!!.root
        ViewCompat.setOnApplyWindowInsetsListener(rootView.findViewById(R.id.fab_welcome_finish_next)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                if (bottomMargin < systemBars.bottom){
                    bottomMargin = systemBars.bottom + v.marginBottom
                }
            }
            rootView.findViewById<AppBarLayout>(R.id.container_welcome_finish_appbar)
                .setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        rootView.findViewById<FloatingActionButton>(R.id.fab_welcome_finish_next).setOnClickListener {
            lifecycleScope.launch {
                val preferencesHelper = PreferencesHelper(requireContext())
                preferencesHelper.setSettingsFinished()
            }
            val intent = Intent(activity, StartActivity::class.java)
            startActivity(intent)
        }
        _binding!!.topAppBarWelcomeFinish.setNavigationOnClickListener {
            val viewPager = requireActivity().findViewById<ViewPager2>(R.id.pager)
            viewPager.setCurrentItem(viewPager.currentItem - 1, true)
        }
        return rootView
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment WelcomeFinishFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            WelcomeFinishFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }
}