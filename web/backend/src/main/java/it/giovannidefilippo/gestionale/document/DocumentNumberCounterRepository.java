package it.giovannidefilippo.gestionale.document;

import org.springframework.data.jpa.repository.JpaRepository;

interface DocumentNumberCounterRepository extends JpaRepository<DocumentNumberCounter, DocumentNumberCounterId> {
}
