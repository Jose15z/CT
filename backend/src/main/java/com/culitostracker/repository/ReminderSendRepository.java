package com.culitostracker.repository;

import com.culitostracker.domain.model.ReminderSend;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReminderSendRepository extends JpaRepository<ReminderSend, ReminderSend.Key> {
}
