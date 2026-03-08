package org.example.dzdp_analysis.service.data;

import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import org.example.dzdp_analysis.repository.dao.data.EtlTaskRepository;
import org.example.dzdp_analysis.repository.entity.data.EtlTask;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;

@Service
public class EtlTaskLoggerService {

    @Autowired
    private EtlTaskRepository etlTaskRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW) //  独立事务
    public EtlTask saveTask(EtlTask task) {
        return etlTaskRepository.save(task);
    }
}
