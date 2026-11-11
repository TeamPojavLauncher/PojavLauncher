package net.kdt.pojavlaunch.prefs.screens

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.Button
import androidx.appcompat.widget.SwitchCompat
import net.kdt.pojavlaunch.R
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.mobileglues.MgController
import net.kdt.pojavlaunch.mobileglues.MgDialogHost
import net.kdt.pojavlaunch.mobileglues.settings.MGConfig
import net.kdt.pojavlaunch.mobileglues.settings.MultidrawEntry
import net.kdt.pojavlaunch.mobileglues.settings.MultidrawOrderItem
import kotlinx.coroutines.launch

/**
 * Drag-to-reorder list of MultiDraw strategies. Without the [ARG_ENTRY] argument it edits the
 * global order, with it the independent order of a single GL function.
 */
class MobileGluesMultidrawOrderFragment : Fragment(R.layout.fragment_mg_multidraw_order) {

    private val controller: MgController by lazy { MgController.get(requireContext()) }
    private var entry: MultidrawEntry? = null

    private val rows = mutableListOf<Row>()
    private var dragging = false
    private var dragEnabled = true
    private var latest: List<Row> = emptyList()
    private lateinit var adapter: OrderAdapter
    private lateinit var touchHelper: ItemTouchHelper

    private data class Row(val label: String, val sublabel: String?)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        entry = arguments?.getString(ARG_ENTRY)?.let { name ->
            MultidrawEntry.entries.firstOrNull { it.name == name }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        controller.ensureLoaded()
        MgDialogHost(this, controller).attach()
        val entry = entry

        view.findViewById<View>(R.id.mg_back_row).setOnClickListener {
            ExtraCore.setValue(ExtraConstants.BACK_PREFERENCE, "true")
        }
        view.findViewById<TextView>(R.id.mg_order_title).text =
            entry?.glFunction ?: getString(R.string.md_global_order_title)

        val exceptionSwitch = view.findViewById<SwitchCompat>(R.id.mg_order_exception_switch)
        if (entry != null) {
            exceptionSwitch.visibility = View.VISIBLE
            exceptionSwitch.setOnCheckedChangeListener { _, checked -> controller.setException(entry, checked) }
        }

        val bench = view.findViewById<Button>(R.id.mg_order_bench)
        bench.setText(if (entry == null) R.string.md_bench_run_all else R.string.md_bench_run_entry)
        bench.setOnClickListener {
            controller.runBench(
                if (entry == null) MgController.BenchTarget.AllEntries else MgController.BenchTarget.Entry(entry),
            )
        }
        view.findViewById<Button>(R.id.mg_order_reset).setOnClickListener {
            if (entry == null) controller.resetGlobalOrder() else controller.resetExceptionOrder(entry)
        }

        adapter = OrderAdapter()
        touchHelper = ItemTouchHelper(DragCallback())
        view.findViewById<RecyclerView>(R.id.mg_order_list).apply {
            layoutManager = LinearLayoutManager(requireContext())
            adapter = this@MobileGluesMultidrawOrderFragment.adapter
            touchHelper.attachToRecyclerView(this)
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                controller.config.collect { bind(view, it) }
            }
        }
    }

    override fun onStop() {
        super.onStop()
        controller.flushAsync()
    }

    private fun bind(root: View, config: MGConfig?) {
        if (config == null) return
        val settings = config.multidraw
        val entry = entry
        val reset = root.findViewById<View>(R.id.mg_order_reset)

        if (entry == null) {
            latest = settings.globalOrder.map { item ->
                Row(
                    item.label(requireContext()).toString(),
                    if (item == MultidrawOrderItem.Native) getString(R.string.md_item_native_desc) else null,
                )
            }
            dragEnabled = true
            reset.visibility = if (settings.globalCustomized) View.VISIBLE else View.GONE
        } else {
            latest = settings.effectiveOrderFor(entry).map { Row(it.label(requireContext()).toString(), null) }
            val enabled = settings.hasException(entry)
            dragEnabled = enabled
            val toggle = root.findViewById<SwitchCompat>(R.id.mg_order_exception_switch)
            if (toggle.isChecked != enabled) toggle.isChecked = enabled
            reset.visibility = if (enabled && settings.exceptionCustomized(entry)) View.VISIBLE else View.GONE
        }
        if (!dragging) submit(latest)
    }

    private fun submit(newRows: List<Row>) {
        if (newRows == rows) {
            adapter.notifyItemRangeChanged(0, rows.size)
            return
        }
        rows.clear()
        rows.addAll(newRows)
        adapter.notifyDataSetChanged()
    }

    private inner class DragCallback : ItemTouchHelper.SimpleCallback(ItemTouchHelper.UP or ItemTouchHelper.DOWN, 0) {
        override fun isLongPressDragEnabled() = false

        override fun onMove(rv: RecyclerView, from: RecyclerView.ViewHolder, to: RecyclerView.ViewHolder): Boolean {
            if (!dragEnabled) return false
            val fromPos = from.bindingAdapterPosition
            val toPos = to.bindingAdapterPosition
            if (fromPos == RecyclerView.NO_POSITION || toPos == RecyclerView.NO_POSITION) return false
            rows.add(toPos, rows.removeAt(fromPos))
            adapter.notifyItemMoved(fromPos, toPos)
            val entry = entry
            if (entry == null) controller.moveGlobalItem(fromPos, toPos) else controller.moveExceptionItem(entry, fromPos, toPos)
            return true
        }

        override fun onSwiped(holder: RecyclerView.ViewHolder, direction: Int) = Unit

        override fun onSelectedChanged(holder: RecyclerView.ViewHolder?, actionState: Int) {
            super.onSelectedChanged(holder, actionState)
            if (actionState == ItemTouchHelper.ACTION_STATE_DRAG) dragging = true
        }

        override fun clearView(rv: RecyclerView, holder: RecyclerView.ViewHolder) {
            super.clearView(rv, holder)
            dragging = false
            submit(latest)
        }
    }

    private inner class OrderAdapter : RecyclerView.Adapter<OrderAdapter.Holder>() {

        inner class Holder(view: View) : RecyclerView.ViewHolder(view) {
            val position: TextView = view.findViewById(R.id.mg_order_position)
            val label: TextView = view.findViewById(R.id.mg_order_label)
            val sublabel: TextView = view.findViewById(R.id.mg_order_sublabel)
            val handle: ImageView = view.findViewById(R.id.mg_order_handle)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder =
            Holder(LayoutInflater.from(parent.context).inflate(R.layout.item_mg_order_row, parent, false))

        @SuppressLint("ClickableViewAccessibility")
        override fun onBindViewHolder(holder: Holder, index: Int) {
            val row = rows[index]
            holder.position.text = (index + 1).toString()
            holder.label.text = row.label
            holder.sublabel.visibility = if (row.sublabel == null) View.GONE else View.VISIBLE
            holder.sublabel.text = row.sublabel
            holder.handle.alpha = if (dragEnabled) 1f else 0.38f
            holder.handle.setOnTouchListener { _, event ->
                if (dragEnabled && event.actionMasked == MotionEvent.ACTION_DOWN) touchHelper.startDrag(holder)
                false
            }
        }

        override fun getItemCount() = rows.size
    }

    companion object {
        const val ARG_ENTRY = "mg_multidraw_entry"
    }
}
