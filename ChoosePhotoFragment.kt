package com.example.animalApp.choose_photo_screen

import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.setupWithNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.animalApp.R
import com.example.animalApp.databinding.FragmentChoosePhotoBinding


class ChoosePhotoFragment : Fragment(), ChoosePhotoProvider.ViewProvider,
    ChoosePhotoProvider.RVAdapterProvider {

    private lateinit var mBinding: FragmentChoosePhotoBinding
    private lateinit var mPresenter: ChoosePhotoPresenter
    private var mPhotoRVAdapter: ChoosePhotoRVAdapter? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mBinding =
            DataBindingUtil.inflate(inflater, R.layout.fragment_choose_photo, container, false)
        return mBinding.root
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onResume() {
        super.onResume()
        mPresenter.onResume()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mPresenter = ChoosePhotoPresenter(requireActivity(), this)

        mBinding.toolChoosePhoto.setupWithNavController(
            findNavController(),
            AppBarConfiguration(findNavController().graph)
        )
        (requireActivity() as AppCompatActivity).setSupportActionBar(mBinding.toolChoosePhoto)

        mBinding.fabChoosePhotoCamera.setOnClickListener {
            mPresenter.onShowCameraOpen()
        }

        mPhotoRVAdapter = ChoosePhotoRVAdapter(this@ChoosePhotoFragment)
        mBinding.rvChoosePhoto.apply {
            adapter = mPhotoRVAdapter
            layoutManager = GridLayoutManager(requireContext(), 3)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.menu_choose_photo, menu)
        super.onCreateOptionsMenu(menu, inflater)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.imChoosePhotoSettings) {
            mPresenter.onSettingsClick()
        }
        return true
    }

    override fun setListViews(imagesList: ArrayList<Bitmap>) {
        activity?.runOnUiThread {
            mPhotoRVAdapter!!.addItems(imagesList)
        }
    }


    override fun showSetting() {
        findNavController().navigate(ChoosePhotoFragmentDirections.actionChoosePhotoFragmentToSettingsFragment())
    }

    override fun showCamera() {
        val action = ChoosePhotoFragmentDirections.actionChoosePhotoFragmentToCameraFragment()
        findNavController().navigate(action)
    }

    override fun showNext(path: String, from: Int) {
        findNavController().navigate(
            ChoosePhotoFragmentDirections.actionChoosePhotoFragmentToCalculateFragment(
                path,
                from
            )
        )
    }

    override fun clearRecyclerView() {
        activity?.runOnUiThread {
            mPhotoRVAdapter?.clear()
        }
    }

    override fun showError(error: String) {
        activity?.runOnUiThread {
            showMessage(error)
        }
    }

    override fun showMessage(message: String) {
        activity?.runOnUiThread {
            Toast.makeText(activity, message, Toast.LENGTH_LONG).show()
        }
    }

    override fun showAnimation() {
        activity?.runOnUiThread {
            mBinding.progressChoosePhoto.show()
        }
    }

    override fun hideAnimation() {
        activity?.runOnUiThread {
            mBinding.progressChoosePhoto.hide()
        }
    }

    override fun onItemClick(position: Int) {
        mPresenter.onImageClick(position)
    }

    override fun onLastItemShowed() {
        mPresenter.onLastItemShowed()
    }

}