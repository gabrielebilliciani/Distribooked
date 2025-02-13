package it.unipi.distribooked.repository.mongo.views;

import java.util.List;

public interface LibraryCopiesView {
    List<BranchView> getBranches();

    interface BranchView {
        int getNumberOfCopies();
    }
}
