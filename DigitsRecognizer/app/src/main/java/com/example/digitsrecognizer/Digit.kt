package com.example.digitsrecognizer

import android.graphics.Bitmap
import android.os.Parcel
import android.os.Parcelable

data class Digit(var resultPredicted: Int, var confidence: Float, var pictureImage: Bitmap): Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readInt(),
        parcel.readFloat(),
        Bitmap.CREATOR.createFromParcel(parcel)
    )
    //Parceling to allow send data between activity
    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(resultPredicted)
        parcel.writeFloat(confidence)
        pictureImage.writeToParcel(parcel,0)
    }
    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Digit> {
        override fun createFromParcel(parcel: Parcel): Digit {
            return Digit(parcel)
        }
        override fun newArray(size: Int): Array<Digit?> {
            return arrayOfNulls(size)
        }
    }
}

