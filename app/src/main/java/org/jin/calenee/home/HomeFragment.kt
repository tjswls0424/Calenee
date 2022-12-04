package org.jin.calenee.home

import android.content.Context
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import org.jin.calenee.MainActivity
import org.jin.calenee.R
import org.jin.calenee.databinding.FragmentHomeBinding

class HomeFragment : Fragment(R.layout.fragment_home) {
    private lateinit var binding: FragmentHomeBinding
    private lateinit var mContext: Context
    private lateinit var coupleInfoViewModel: CoupleInfoViewModel

    private val firestore by lazy {
        Firebase.firestore
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is MainActivity) {
            mContext = context
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        coupleInfoViewModel =
            ViewModelProvider(activity as ViewModelStoreOwner)[CoupleInfoViewModel::class.java]

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate<FragmentHomeBinding?>(
            inflater,
            R.layout.fragment_home,
            container,
            false
        ).apply {
            viewModel = coupleInfoViewModel
        }

        return binding.root
    }
}