package com.clausify.contract;

import com.clausify.common.NotFoundException;
import com.clausify.common.PageResponse;
import com.clausify.common.UnsupportedFileTypeException;
import com.clausify.user.User;
import com.clausify.user.UserRepository;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;

@Service
public class ContractService {

    static final String ONLY_PDF = "Only PDF files are supported.";
    static final String NOT_FOUND = "Contract not found.";
    /** Newest first; id breaks ties between uploads in the same instant so pages never overlap. */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("uploadedAt"), Sort.Order.desc("id"));
    /** Every PDF starts with these bytes; the declared content type alone can be faked. */
    private static final byte[] PDF_SIGNATURE = "%PDF-".getBytes(StandardCharsets.US_ASCII);
    private static final int MAX_FILENAME_LENGTH = 255;

    private final ContractRepository contractRepository;
    private final UserRepository userRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ContractService(ContractRepository contractRepository, UserRepository userRepository,
                           PdfTextExtractor pdfTextExtractor, ApplicationEventPublisher events, Clock clock) {
        this.contractRepository = contractRepository;
        this.userRepository = userRepository;
        this.pdfTextExtractor = pdfTextExtractor;
        this.events = events;
        this.clock = clock;
    }

    /**
     * FR-3: checks the file is a PDF, extracts its text in memory, and saves the contract as UPLOADED.
     * Nothing is saved if any step fails. Analysis starts from the published event once this commits.
     */
    @Transactional
    public ContractSummaryResponse upload(MultipartFile file, Long userId) {
        byte[] bytes = readPdf(file);
        ExtractedDocument document = pdfTextExtractor.extract(bytes);
        User owner = userRepository.findById(userId).orElseThrow(() -> new NotFoundException("User not found."));

        Contract contract = contractRepository.save(Contract.uploaded(owner, cleanFilename(file.getOriginalFilename()),
                bytes.length, document.pageCount(), document.text(), clock.instant()));
        events.publishEvent(new ContractUploadedEvent(contract.getId()));
        return ContractSummaryResponse.from(contract);
    }

    /** FR-4: the caller's contracts, newest first. Clients choose page and size, never the sort. */
    @Transactional(readOnly = true)
    public PageResponse<ContractSummaryResponse> list(Long userId, Pageable pageable) {
        Pageable newestFirst = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), NEWEST_FIRST);
        return PageResponse.of(contractRepository.findAllByUserId(userId, newestFirst), ContractSummaryResponse::from);
    }

    @Transactional(readOnly = true)
    public ContractDetailResponse get(Long id, Long userId) {
        return ContractDetailResponse.from(findOwned(id, userId));
    }

    /**
     * The only way to load a contract for a user request. Another user's contract throws the same 404 as
     * a missing one, so ids cannot be probed (CLAUDE.md "Ownership").
     */
    Contract findOwned(Long id, Long userId) {
        return contractRepository.findByIdAndUserId(id, userId).orElseThrow(() -> new NotFoundException(NOT_FOUND));
    }

    private static byte[] readPdf(MultipartFile file) {
        if (file.isEmpty() || !isPdfContentType(file.getContentType())) {
            throw new UnsupportedFileTypeException(ONLY_PDF);
        }
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException ex) {
            throw new UncheckedIOException("Could not read the uploaded file", ex);
        }
        if (bytes.length < PDF_SIGNATURE.length
                || !Arrays.equals(bytes, 0, PDF_SIGNATURE.length, PDF_SIGNATURE, 0, PDF_SIGNATURE.length)) {
            throw new UnsupportedFileTypeException(ONLY_PDF);
        }
        return bytes;
    }

    private static boolean isPdfContentType(String contentType) {
        if (contentType == null) {
            return false;
        }
        try {
            return MediaType.APPLICATION_PDF.isCompatibleWith(MediaType.parseMediaType(contentType));
        } catch (IllegalArgumentException ex) {
            return false;
        }
    }

    /** Keeps only the file's own name (some browsers send a full path) and drops control characters. */
    static String cleanFilename(String original) {
        String name = original == null ? "" : original.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "").strip();
        if (name.isEmpty()) {
            name = "contract.pdf";
        }
        return name.length() <= MAX_FILENAME_LENGTH ? name : name.substring(name.length() - MAX_FILENAME_LENGTH);
    }
}
