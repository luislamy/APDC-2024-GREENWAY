package pt.unl.fct.di.apdc.projeto.util;

import java.util.ArrayList;
import java.util.List;

public class CommentNode {

    public CommentData commentData;

    public List<CommentNode> children;

    public CommentNode() {}

    public CommentNode(CommentData commentData) {
        this.commentData = commentData;
        this.children = new ArrayList<>();
    }
}