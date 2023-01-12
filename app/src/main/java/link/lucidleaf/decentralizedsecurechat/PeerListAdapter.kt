package link.lucidleaf.decentralizedsecurechat

import android.net.wifi.p2p.WifiP2pDevice
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

class PeerListAdapter(
    private val dataSet: MutableList<WifiP2pDevice>,
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
            .inflate(R.layout.peer_list_peer_name, viewGroup, false)
        return PeerViewHolder(view)
    }

    // Replace the contents of a view (invoked by the layout manager)
    override fun onBindViewHolder(viewHolder: PeerViewHolder, position: Int) {
        // Get element from dataset at this position and replace the
        // contents of the view with that element
        val otherDevice = dataSet[position]
        viewHolder.textView.text = otherDevice.deviceName
        viewHolder.textView.setOnClickListener {
            activity.openChat(otherDevice)
        }
    }

    override fun getItemCount() = dataSet.size

}