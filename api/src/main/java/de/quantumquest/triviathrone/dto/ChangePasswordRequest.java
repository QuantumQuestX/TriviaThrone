package de.quantumquest.triviathrone.dto;

public record ChangePasswordRequest(String oldPassword, String newPassword) {}
