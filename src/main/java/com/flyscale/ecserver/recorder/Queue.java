package com.flyscale.ecserver.recorder;

/**
 * Created by bian on 2019/1/23.
 * 使用双向链表定义一个队列
 */

public class Queue<T> {

    private Node mHead; //head是一个空结点
    private Node mTail;//尾结点
    private boolean mEnabled;//默认不可用

    public Queue() {
        mHead = new Node(null);
        mTail = mHead;
        mHead.next = mTail;
        mTail.pre = mHead;
        mEnabled = false;//默认不可用，如果要出入栈需要进行设置
    }

    /**
     * 在队列头部删除
     *
     * @return
     */
    public T pop() {
        Node node;
        if (mHead.next != mTail && mEnabled) {
            node = mHead.next;
            mHead.next = mHead.next.next;
            node.next.pre = mHead;
            return node.t;
        }
        return null;
    }

    /**
     * 在队列尾部插入
     *
     * @param t
     */
    public void push(T t) {
        if (mEnabled) {
            Node node = new Node(t);
            node.next = mTail;
            node.pre = mTail.pre;
            mTail.pre.next = node;
            mTail.pre = node;
        }
    }

    public int length() {
        Node node;
        node = mHead;
        int length = 0;
        while (node.next != mTail) {
            length++;
            node = node.next;
        }
        return length;
    }

    @Override
    public String toString() {
        Node node = mHead.next;
        StringBuilder sb = new StringBuilder();
        while (node != mTail) {
            sb.append(node.t.toString() + "->");
            node = node.next;
        }
        sb.append("null");
        return sb.toString();
    }

    public void clear() {
        mHead = new Node(null);
        mTail = mHead;
        mHead.next = mTail;
        mTail.pre = mHead;
    }

    public void setEnabled(boolean enabled) {
        this.mEnabled = enabled;
    }

    public boolean isEnabled(){
        return mEnabled;
    }

    private class Node {
        public Node(T t) {
            this.t = t;
        }

        public Node next;
        public Node pre;
        public T t;
    }
}
