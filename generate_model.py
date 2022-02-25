import tensorflow as tf
from tensorflow import keras

import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import random

def create_model():
  model = keras.Sequential([
    keras.layers.InputLayer(input_shape=(28, 28, 1)),
    keras.layers.Conv2D(filters=32, kernel_size=(3, 3), activation=tf.nn.relu),
    keras.layers.Conv2D(filters=64, kernel_size=(3, 3), activation=tf.nn.relu),
    keras.layers.MaxPooling2D(pool_size=(2, 2)),
    keras.layers.Dropout(0.25),
    keras.layers.Flatten(),
    keras.layers.Dense(10, activation=tf.nn.softmax)
  ])
  model.compile(optimizer='adam',
                loss='sparse_categorical_crossentropy',
                metrics=['accuracy'])
  return model


mnist = keras.datasets.mnist
(train_images, train_labels), (test_images, test_labels) = mnist.load_data()

train_images = train_images / 255.0
test_images = test_images / 255.0

train_images = np.expand_dims(train_images, axis=3)
test_images = np.expand_dims(test_images, axis=3)



datagen = keras.preprocessing.image.ImageDataGenerator(
  rotation_range=20,
  width_shift_range = 0.2, # also good 0.0 
  shear_range=0.25,
  zoom_range=-0.1 # the best (for this app)
)

train_generator = datagen.flow(train_images, train_labels)
test_generator = datagen.flow(test_images, test_labels)

base_model.evaluate(test_generator)

improved_model = create_model()
improved_model.fit(train_generator, epochs=5, validation_data=test_generator)

converter = tf.lite.TFLiteConverter.from_keras_model(improved_model)
converter.optimizations = [tf.lite.Optimize.DEFAULT]
tflite_quantized_model = converter.convert()

f = open('mnist.tflite', "wb")
f.write(tflite_quantized_model)
f.close()
