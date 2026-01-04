package com.career.agent.repo;

import com.career.agent.model.JobMatch;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface JobMatchRepository extends JpaRepository<JobMatch, Long> {
	List<JobMatch> findByUserEmail(String userEmail);

	List<JobMatch> findByTitleContainingAndCityContaining(String title, String city);
}
