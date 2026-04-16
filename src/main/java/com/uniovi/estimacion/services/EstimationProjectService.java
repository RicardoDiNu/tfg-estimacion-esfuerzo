package com.uniovi.estimacion.services;

import com.uniovi.estimacion.entities.EstimationProject;
import com.uniovi.estimacion.repositories.EstimationProjectRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class EstimationProjectService {

    private final EstimationProjectRepository estimationProjectRepository;

    public List<EstimationProject> getProjects() {
        List<EstimationProject> projects = new ArrayList<>();
        estimationProjectRepository.findAll().forEach(projects::add);
        return projects;
    }

    public Optional<EstimationProject> getProject(Long id) {
        return estimationProjectRepository.findById(id);
    }

    public void saveProject(EstimationProject estimationProject) {
        estimationProjectRepository.save(estimationProject);
    }

    public void deleteProject(Long id) {
        estimationProjectRepository.deleteById(id);
    }
}