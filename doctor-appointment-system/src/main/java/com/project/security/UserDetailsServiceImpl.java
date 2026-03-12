package com.project.security;

import com.project.entity.User;
import com.project.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    UserRepository userRepository;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        // 'identifier' could be an email OR a mobile number
        User user = userRepository.findByEmail(identifier)
                .orElseGet(() -> userRepository.findByMobileNumber(identifier)
                        .orElseThrow(() -> new UsernameNotFoundException("User Not Found with email or mobile: " + identifier)));

        return UserDetailsImpl.build(user);
    }
}
