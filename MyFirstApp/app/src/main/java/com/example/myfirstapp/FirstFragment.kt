package com.example.myfirstapp

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.navigation.fragment.findNavController
import com.example.myfirstapp.databinding.FragmentFirstBinding
import org.w3c.dom.Text

/**
 * A simple [Fragment] subclass as the default destination in the navigation.
 */
class FirstFragment : Fragment() {

    private var _binding: FragmentFirstBinding? = null

    // This property is only valid between onCreateView and
    // onDestroyView.
    private val binding get() = _binding!!

    private fun countMe(view: View){
        val showCountTextView = view.findViewById<TextView>(R.id.textview_first)
        val countString = showCountTextView.text.toString()
        var count = countString.toInt()
        count++
        showCountTextView.text = count.toString()
    }

    private fun resetMe(view: View){
        val resetCountTextView = view.findViewById<TextView>(R.id.textview_first)
        resetCountTextView.text = 0.toString()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        _binding = FragmentFirstBinding.inflate(inflater, container, false)
        return binding.root

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.randomButton.setOnClickListener {
            findNavController().navigate(R.id.action_FirstFragment_to_SecondFragment)
        }

        binding.toastButton.setOnClickListener{
            val myToast = Toast.makeText(context, "Hello Toast!", Toast.LENGTH_SHORT)
            myToast.show()
        }

        binding.countButton.setOnClickListener{
            countMe(view)
        }

        binding.resetButton.setOnClickListener{
            resetMe(view)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}