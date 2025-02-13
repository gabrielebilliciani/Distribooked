package it.unipi.distribooked.repository.mongo.views;

import it.unipi.distribooked.model.embedded.Address;

import java.time.LocalDate;

/**
 * Projection for retrieving user profile details.
 */
public interface UserDetailsView {

    String getId();

    String getUsername();

    String getName();

    String getSurname();

    LocalDate getDateOfBirth();

    String getEmail();

    Address getAddress();
}
