package com.uteq.SCLI.dto;

public record CounterDTO(
  int totalAsignadas, int totalRestantes,
  int matutinaAsignadas, int matutinaRestantes,
  int vespertinaAsignadas, int vespertinaRestantes
) {}

