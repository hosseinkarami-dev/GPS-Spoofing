package ir.elsadesign.fakegps.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.content.ContextCompat
import ir.elsadesign.fakegps.R
import ir.elsadesign.fakegps.databinding.SelectWayListItemBinding
import ir.elsadesign.fakegps.helpers.HelperMain

class SelectWayListAdapter(
    context: Context,
    items: ArrayList<Item>
) : ArrayAdapter<SelectWayListAdapter.Item>(context, R.layout.select_way_list_item, items) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = getItem(position)
        var convertView1 = convertView

        val binding: SelectWayListItemBinding

        if (convertView1 == null) {
            binding =
                SelectWayListItemBinding.inflate(LayoutInflater.from(context), parent, false)
            convertView1 = binding.root
            convertView1.tag = binding
        } else
            binding = convertView1.tag as SelectWayListItemBinding

        item?.let {
            binding.text.text = item.text
            HelperMain.setIcon(
                context,
                binding.icon,
                item.iconRes,
                ContextCompat.getColor(context, R.color.fontGrayColor)
            )
        }

        return convertView1
    }

    data class Item(
        val id: Int,
        val text: String,
        val iconRes: Int
    )
}