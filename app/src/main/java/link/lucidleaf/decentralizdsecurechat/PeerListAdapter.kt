package link.lucidleaf.decentralizdsecurechat

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView

class PeerListAdapter(
    private val dataSet: Array<String>,
    private val activity: MainActivity,
) :
    RecyclerView.Adapter<PeerListAdapter.PeerViewHolder>() {

    /**
     * Provide a reference to the type of views that you are using
     * (custom ViewHolder).
     */
    class PeerViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val textView: TextView

        init {
            // Define click listener for the ViewHolder's View.
            textView = view.findViewById(R.id.peerListName)
        }

    }

    // Create new views (invoked by the layout manager)
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int): PeerViewHolder {
        // Create a new view, which defines the UI of the list item
        val view = LayoutInflater.from(viewGroup.context)
            .inflate(R.layout.peer_list_item, viewGroup, false)

        return PeerViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: PeerViewHolder, position: Int) {

        // Get element from your dataset at this position and replace the
        // contents of the view with that element
        val peer = dataSet[position]
        viewHolder.textView.text = peer
        viewHolder.textView.setOnClickListener {
            activity.openChat(peer)
        }
    }

    // Return the size of your dataset (invoked by the layout manager)
    override fun getItemCount() = dataSet.size

}