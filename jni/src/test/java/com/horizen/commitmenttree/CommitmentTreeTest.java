package com.horizen.commitmenttree;

import org.junit.Test;
import static org.junit.Assert.*;
import com.horizen.commitmenttree.CommitmentTree;
import com.horizen.sigproofnative.BackwardTransfer;
import com.horizen.librustsidechains.FieldElement;

import java.util.Arrays;
import java.util.Optional;
import java.util.Random;

public class CommitmentTreeTest {
    byte[] generateFieldElementBytes() {
        try (FieldElement tmp = FieldElement.createRandom()) {
            return tmp.serializeFieldElement();
        }
    }


    byte[] generateRandomBytes(int len) {
        byte[] bytes = new byte[len];
        new Random().nextBytes(bytes);
        return bytes;
    }

    @Test
    public void createAndFree() {
        CommitmentTree commTree = CommitmentTree.init();
        commTree.freeCommitmentTree();
    }

    @Test
    public void addScCreation() {
        CommitmentTree commTree = CommitmentTree.init();
        byte[] scId = generateFieldElementBytes();

        assertFalse("ScCr commitment expected to be missed.", commTree.getScCrCommitment(scId).isPresent());

        long amount = 100;
        byte[] pubKey = generateRandomBytes(32);
        int withdrawalEpochLength = 1000;
        byte[] customData = generateRandomBytes(1024);
        Optional<byte[]> constant = Optional.of(generateFieldElementBytes());
        byte[] certVk = new byte[1]; // todo
        Optional<byte[]> btrVk = Optional.empty();
        Optional<byte[]> cswVk = Optional.empty();
        byte[] txHash = generateRandomBytes(32);
        int outIdx = 0;

        assertTrue("Sidechain creation output expected to be added.",
                commTree.addScCr(scId, amount, pubKey, withdrawalEpochLength,
                        customData, constant, certVk, btrVk, cswVk, txHash, outIdx)
        );

        Optional<FieldElement> commitmentOpt = commTree.getScCrCommitment(scId);
        assertTrue("ScCr commitment expected to be present.", commitmentOpt.isPresent());
        commTree.freeCommitmentTree();
    }

    @Test
    public void addForwardTransfer() {
        CommitmentTree commTree = CommitmentTree.init();
        byte[] scId = generateFieldElementBytes();

        assertFalse("Forward transfer output expected to be missed.", commTree.getFwtCommitment(scId).isPresent());

        long ftrAmount = 100;
        byte[] ftrPublikKeyHash = generateFieldElementBytes();
        byte[] ftrTransactionHash = generateFieldElementBytes();
        int fwtOutId = 200;
        assertTrue("Forward transfer output expected to be added.",
                commTree.addFwt(scId, ftrAmount, ftrPublikKeyHash, ftrTransactionHash, fwtOutId));

        Optional<FieldElement> commitmentOpt = commTree.getFwtCommitment(scId);
        assertTrue("Forward transfer expected to be present.", commitmentOpt.isPresent());
        commTree.freeCommitmentTree();
    }

    @Test
    public void addBackwardTransfer() {
        CommitmentTree commTree = CommitmentTree.init();
        byte[] scId = generateFieldElementBytes();

        assertFalse("Backward transfer output expected to be missed.", commTree.getBtrCommitment(scId).isPresent());

        long bwtAmount = 120;
        byte[] bwtPublikKeyHash = generateFieldElementBytes();
        byte[] bwtRequestData = generateFieldElementBytes();
        byte[] bwtTransactionHash = generateFieldElementBytes();
        int bwtOutId = 220;
        assertTrue("Backward transfer output expected to be added.", commTree.addBtr(scId, bwtAmount, bwtPublikKeyHash, bwtRequestData, bwtTransactionHash, bwtOutId));

        Optional<FieldElement> commitmentOpt = commTree.getBtrCommitment(scId);
        assertTrue("Backward transfer expected to be present.", commitmentOpt.isPresent());
        commTree.freeCommitmentTree();
    }

    @Test
    public void addCeasedSidechainWithdrawal() {
        CommitmentTree commTree = CommitmentTree.init();
        byte[] scId = generateFieldElementBytes();

        assertFalse("Ceased Sidechain Withdrawal output expected to be missed.", commTree.getCswCommitment(scId).isPresent());

        long cswAmount = 140;
        byte[] cswPublikKeyHash = generateFieldElementBytes();
        byte[] cswNullifier = generateFieldElementBytes();
        byte[] cswCertificate  = generateFieldElementBytes();
        assertTrue("Ceased Sidechain Withdrawal output expected to be added.", commTree.addCsw(scId, cswAmount, cswNullifier, cswPublikKeyHash, cswCertificate));

        Optional<FieldElement> commitmentOpt = commTree.getCswCommitment(scId);
        assertTrue("Ceased Sidechain Withdrawal expected to be present.", commitmentOpt.isPresent());
        commTree.freeCommitmentTree();
    }

    @Test
    public void addCertificate() {
        CommitmentTree commTree = CommitmentTree.init();
        byte[] scId = generateFieldElementBytes();

        assertFalse("Certificate expected to be missed.", commTree.getCertCommitment(scId).isPresent());

        int cert_epoch = 220;
        long cert_quality = 50;
        byte[] certDataHash = generateFieldElementBytes();
        byte[] certMerkelRoot = generateFieldElementBytes();
        byte[] certCumulativeCommTreeHash = generateFieldElementBytes();

        assertTrue("Certificate output expected to be added.", commTree.addCert(scId, cert_epoch, cert_quality, certDataHash, new BackwardTransfer[0], certMerkelRoot, certCumulativeCommTreeHash));
        Optional<FieldElement> commitmentOpt = commTree.getCertCommitment(scId);
        assertTrue("Certificate expected to be present.", commitmentOpt.isPresent());
        commTree.freeCommitmentTree();
    }

    @Test
    public void addCertificateLeaf() {
        CommitmentTree commTree = CommitmentTree.init();
        byte[] scId = generateFieldElementBytes();

        assertFalse("Certificate expected to be missed.", commTree.getCertCommitment(scId).isPresent());

        byte[] leaf = generateFieldElementBytes();
        assertTrue("Certificate leaf expected to be added.", commTree.addCertLeaf(scId, leaf));

        Optional<FieldElement> commitmentOpt = commTree.getCertCommitment(scId);
        assertTrue("Certificate expected to be present.", commitmentOpt.isPresent());
        commTree.freeCommitmentTree();
    }

    @Test
    public void existanceProofTest() {
        CommitmentTree commTree = CommitmentTree.init();
        byte[] scId = generateFieldElementBytes();

        assertFalse("Forward transfer output expected to be missed.", commTree.getFwtCommitment(scId).isPresent());

        long ftrAmount = 100;
        byte[] ftrPublikKeyHash = generateFieldElementBytes();
        byte[] ftrTransactionHash = generateFieldElementBytes();
        int fwtOutId = 200;
        assertTrue("Forward transfer output expected to be added.",
                commTree.addFwt(scId, ftrAmount, ftrPublikKeyHash, ftrTransactionHash, fwtOutId));

        Optional<FieldElement> commitmentOpt = commTree.getCommitment();
        assertTrue("Tree commitment expected to be present.", commitmentOpt.isPresent());

        // Existence proof
        byte[] absent_scid = generateFieldElementBytes();
        assertFalse("Existence proof expected to be missed", commTree.getScExistenceProof(absent_scid).isPresent());
        Optional<ScExistenceProof> existenceOpt = commTree.getScExistenceProof(scId);
        if (existenceOpt.isPresent()) {
            FieldElement sc_commitment = commTree.getScCommitment(scId).get();
            assertTrue("Commitment verification expected to be successful", CommitmentTree.verifyScCommitment(sc_commitment, existenceOpt.get(), commitmentOpt.get()));
            sc_commitment.freeFieldElement();
        } else {
            assert(false);
        }

        commTree.freeCommitmentTree();
    }

    @Test
    public void absenceProofTest() {
        CommitmentTree commTree = CommitmentTree.init();
        byte[] scId = generateFieldElementBytes();

        Optional<FieldElement> commitmentOpt = commTree.getCommitment();
        assertTrue("Forward transfer expected to be present.", commitmentOpt.isPresent());

        assert(!commTree.getScExistenceProof(scId).isPresent());
        Optional<ScAbsenceProof> absenceOpt = commTree.getScAbsenceProof(scId);
        assertTrue("Absence proof expected to be present.", absenceOpt.isPresent());
        assertTrue("Absence verification expected to be successful", commTree.verifyScAbsence(scId, absenceOpt.get() ,commitmentOpt.get()));

        commTree.freeCommitmentTree();
    }

    @Test
    public void AddTreeDataTest() {
        CommitmentTree commTree = CommitmentTree.init();
//        assert(!commTree.getCommitment().isPresent());

        byte[][] scid = new byte[6][];

        for (int i = 0 ; i < scid.length; i++) {
            scid[i] = Arrays.copyOfRange(generateFieldElementBytes(), 8, 32); // TODO: use 32 bytes
        }

        assert(!commTree.getFwtCommitment(scid[0]).isPresent());
        assert(!commTree.getBtrCommitment(scid[1]).isPresent());
        assert(!commTree.getCswCommitment(scid[2]).isPresent());
        assert(!commTree.getScCrCommitment(scid[3]).isPresent());
        assert(!commTree.getCertCommitment(scid[4]).isPresent());

        FieldElement commitment0 = commTree.getCommitment().get();

        // Add forward transfer
        long ftrAmount = 100;
        byte[] ftrPublikKeyHash = generateFieldElementBytes();
        byte[] ftrTransactionHash = generateFieldElementBytes();
        int fwtOutId = 200;
        assert(commTree.addFwt(scid[0], ftrAmount, ftrPublikKeyHash, ftrTransactionHash, fwtOutId));
        FieldElement commitment1 = commTree.getCommitment().get();
        assert(commitment0 != commitment1);

        // Add backward transfer
        long bwtAmount = 120;
        byte[] bwtPublikKeyHash = generateFieldElementBytes();
        byte[] bwtRequestData = generateFieldElementBytes();
        byte[] bwtTransactionHash = generateFieldElementBytes();
        int bwtOutId = 220;
        assert(commTree.addBtr(scid[1], bwtAmount, bwtPublikKeyHash, bwtRequestData, bwtTransactionHash, bwtOutId));
        FieldElement commitment2 = commTree.getCommitment().get();
//        assert(commitment1 != commitment2);

        // Add ceased sidechain withdrawal
        long cswAmount = 140;
        byte[] cswPublikKeyHash = generateFieldElementBytes();
        byte[] cswNullifier = generateFieldElementBytes();
        byte[] cswCertificate  = generateFieldElementBytes();
        assert(commTree.addCsw(scid[2], cswAmount, cswNullifier, cswPublikKeyHash, cswCertificate));
        FieldElement commitment3 = commTree.getCommitment().get();
        assert(commitment2 != commitment3);

        // Add Sidechain creation commitment
        Random rnd = new Random();
        long crAmount = 160;
        int crEpocLength = 60;
        byte[] crPublikKeyHash = generateFieldElementBytes();
        byte[] crCustomData = generateFieldElementBytes();
        byte[] crConstant = generateFieldElementBytes();
        byte[] crVerificationKey = new byte[1544];
        rnd.nextBytes(crVerificationKey);
        byte[] crTransactionHash = generateFieldElementBytes();
        int crOutId = 240;
        assert(commTree.addScCr(scid[3], crAmount, crPublikKeyHash, crEpocLength, crCustomData, Optional.of(crConstant), crVerificationKey, Optional.empty(), Optional.empty(), crTransactionHash, crOutId));
        FieldElement commitment4 = commTree.getCommitment().get();
        assert(commitment3 != commitment4);

        // Add certificate
        int cert_epoch = 220;
        long cert_quality = 50;
        byte[] certDataHash = generateFieldElementBytes();
        byte[] certMerkelRoot = generateFieldElementBytes();
        byte[] certCumulativeCommTreeHash = generateFieldElementBytes();
        assert (commTree.addCert(scid[4], cert_epoch, cert_quality, certDataHash, new BackwardTransfer[0], certMerkelRoot, certCumulativeCommTreeHash));
        FieldElement commitment5 = commTree.getCommitment().get();
        assert(commitment4 != commitment5);

        byte[] leaf = generateFieldElementBytes();
        assert(commTree.addCertLeaf(scid[5], leaf));
        assert(commTree.getCertCommitment(scid[5]).isPresent());

        // Existence proof
        byte[] absent_scid = generateFieldElementBytes();
        assert(!commTree.getScExistenceProof(absent_scid).isPresent());
        Optional<ScExistenceProof> existenceOpt = commTree.getScExistenceProof(scid[0]);
        if (existenceOpt.isPresent()) {
            FieldElement sc_commitment = commTree.getScCommitment(scid[0]).get();
            assert(CommitmentTree.verifyScCommitment(sc_commitment, existenceOpt.get(), commitment5));
            sc_commitment.freeFieldElement();
        } else {
            assert(false);
        }

        // Absence proof
        assert(!commTree.getScExistenceProof(absent_scid).isPresent());
        Optional<ScAbsenceProof> absenceOpt = commTree.getScAbsenceProof(absent_scid);
        if (absenceOpt.isPresent()) {
            commTree.verifyScAbsence(absent_scid, absenceOpt.get() ,commitment5);
        } else {
            assert(false);
        }

        // Free Field Elements
        commitment0.freeFieldElement();
        commitment1.freeFieldElement();
        commitment2.freeFieldElement();
        commitment3.freeFieldElement();
        commitment4.freeFieldElement();
        commitment5.freeFieldElement();
    }
}