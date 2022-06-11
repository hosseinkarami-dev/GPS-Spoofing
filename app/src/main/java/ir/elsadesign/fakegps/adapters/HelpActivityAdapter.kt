package ir.elsadesign.fakegps.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import ir.elsadesign.fakegps.R
import ir.elsadesign.fakegps.databinding.HelpActivityItemBinding

class HelpActivityAdapter(context: Context, objects: ArrayList<String>) :
    ArrayAdapter<String>(
        context,
        R.layout.help_activity_item,
        objects
    ) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        var convertView1 = convertView
        val binding: HelpActivityItemBinding

        if (convertView1 == null) {
            binding = HelpActivityItemBinding.inflate(LayoutInflater.from(context), parent, false)
            convertView1 = binding.root
            convertView1.tag = binding
        } else
            binding = convertView1.tag as HelpActivityItemBinding

        if (item != null) {
            binding.text.text = item
            binding.number.text = (position + 1).toString()
        }

        return convertView1
    }

    override fun isEnabled(position: Int): Boolean {
        return false
    }
}