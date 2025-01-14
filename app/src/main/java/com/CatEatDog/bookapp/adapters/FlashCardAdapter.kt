package com.CatEatDog.bookapp.adapters


import com.CatEatDog.bookapp.R
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.CatEatDog.bookapp.models.ModelFlashCard
import java.text.SimpleDateFormat
import java.util.*

class FlashCardAdapter(
    private val flashcards: MutableList<ModelFlashCard>,
    private val onDeleteClick: (String) -> Unit,
    private val onFlashcardClick: (String, Int) -> Unit,
    private val studyInterval: Long
) : RecyclerView.Adapter<FlashCardAdapter.FlashCardViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FlashCardViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_flashcard, parent, false)
        return FlashCardViewHolder(view)
    }

    override fun onBindViewHolder(holder: FlashCardViewHolder, position: Int) {
        val flashcard = flashcards[position]
        holder.bind(flashcard)
    }

    override fun getItemCount(): Int = flashcards.size

    inner class FlashCardViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val wordTextView: TextView = itemView.findViewById(R.id.wordTextView)
        private val meaningTextView: TextView = itemView.findViewById(R.id.meaningTextView)
        private val dateTextView: TextView = itemView.findViewById(R.id.dateTextView)
        private val deleteButton: ImageView = itemView.findViewById(R.id.deleteButton)
        private val cardBackground: View = itemView.findViewById(R.id.flashcardBackground)

        fun bind(flashcard: ModelFlashCard) {
            wordTextView.text = "Word: ${flashcard.word}"
            meaningTextView.text = "Meaning: ${flashcard.word_meaning}"

            val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            dateTextView.text = "Date added: ${dateFormat.format(Date(flashcard.timestamp))}"

            val currentTime = System.currentTimeMillis()
            val dueTime = (flashcard.lastStudiedDate ?: 0L) + studyInterval

            // Change background color based on the due date
            if (currentTime >= dueTime) {
                cardBackground.setBackgroundColor(itemView.context.getColor(R.color.red))
            } else {
                cardBackground.setBackgroundColor(itemView.context.getColor(R.color.green))
            }

            itemView.setOnClickListener {
                onFlashcardClick(flashcard.id, bindingAdapterPosition)
            }

            deleteButton.setOnClickListener {
                onDeleteClick(flashcard.id)
            }
        }
    }
}


