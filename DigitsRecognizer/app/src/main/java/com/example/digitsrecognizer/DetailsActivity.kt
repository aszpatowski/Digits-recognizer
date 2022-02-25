package com.example.digitsrecognizer

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.ArrayList

class DetailsActivity: AppCompatActivity()  {
    private var digitListAdapter: DigitListAdapter? = null
    private var digitList = ArrayList<Digit>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_details)

        digitList = intent?.getParcelableArrayListExtra("EXTRA_PREDICTED_DIGIT")?:
                throw IllegalStateException("Digit array list is null")

        val recyclerView: RecyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        digitListAdapter = DigitListAdapter(this,digitList)
        recyclerView.adapter = digitListAdapter

    }

}