# 如何得到Android平台上可用的模型

## 准备训练模型和测试模型所需的数据
```
import pandas as pd
import numpy as np

raw_data = pd.read_csv("data/raw_data.csv").values
X_data = raw_data[:, :12]
y_data = raw_data[:, 12]
chord_classes = {'a':0, 'am':1, 'bm':2, 'c':3, 'd':4, 'dm':5, 'e':6, 'em':7, 'f':8, 'g':9}
y_data_pre = np.array([chord_classes[i] for i in y_data], dtype=np.int32)
```
```
from sklearn.model_selection import train_test_split

train_set, test_set, train_label, test_label = train_test_split(
    X_data,
    y_data_pre,
    test_size=0.2,
    random_state=2333
)
```
```
train_set.shape, test_set.shape, train_label.shape, test_label.shape
Out: ((328376, 12), (82095, 12), (328376,), (82095,))
```

## 构建Tensorflow的图模型
```
import tensorflow as tf
n_inputs = 12
n_hidden1 = 300
n_hidden2 = 100
n_outputs = 10


X = tf.placeholder(tf.float32, shape=(None, n_inputs), name="inputs")
y = tf.placeholder(tf.int64, shape=(None), name="labels")


with tf.name_scope("dnn"):
    hidden1 = tf.layers.dense(X, n_hidden1, name="hidden1",
                             activation=tf.nn.relu)
    hidden2 = tf.layers.dense(hidden1, n_hidden2, name="hidden2",
                             activation=tf.nn.relu)
    logits = tf.layers.dense(hidden2, n_outputs, name="outputs")


with tf.name_scope("loss"):
    xentropy = tf.nn.sparse_softmax_cross_entropy_with_logits(labels=y, logits=logits)
    loss = tf.reduce_mean(xentropy, name="loss")


learning_rate = 0.01

with tf.name_scope("train"):
    optimizer = tf.train.GradientDescentOptimizer(learning_rate)
    training_op = optimizer.minimize(loss)


with tf.name_scope("eval"):
    correct = tf.nn.in_top_k(logits, y, 1)
    accuracy = tf.reduce_mean(tf.cast(correct, tf.float32))


output = tf.nn.softmax(logits=logits, name="outputs")


init = tf.global_variables_initializer()
saver = tf.train.Saver()
```

## 使用数据训练模型，并将模型保存
```
n_epochs = 30
batch_size = 50

indices = np.arange(train_set.shape[0])
np.random.shuffle(indices)

with tf.Session() as sess:
    init.run()
    for epoch in range(n_epochs):
        np.random.shuffle(indices)
        for batch_index in np.array_split(indices, 3000):
            X_batch, y_batch = train_set[batch_index], train_label[batch_index]
            sess.run(training_op, feed_dict={X: X_batch, y: y_batch})
        acc_train = accuracy.eval(feed_dict={X: X_batch, y: y_batch})
        acc_test = accuracy.eval(feed_dict={X: test_set, 
                                            y: test_label})
        print epoch, "Train accuracy:", acc_train, "Test accuracy:", acc_test
    save_path = saver.save(sess, "chord_model/chord_2.ckpt")
```

## 将模型由ckpt格式转换为android平台可用的pb格式文件
```
saver = tf.train.import_meta_graph("chord_model/chord_2.ckpt.meta", clear_devices=True)
input_graph_def = tf.get_default_graph().as_graph_def()

with tf.Session() as sess:
    saver.restore(sess, "chord_model/chord_2.ckpt")
    output_graph_def = tf.graph_util.convert_variables_to_constants(
        sess,
        input_graph_def,
        ["outputs"]
    )
    with tf.gfile.GFile("chord_model/chord_2.pb", "wb") as f:
        f.write(output_graph_def.SerializeToString())
```
