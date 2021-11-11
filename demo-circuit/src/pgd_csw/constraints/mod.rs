use cctp_primitives::type_mapping::FieldElement;
use r1cs_core::{ConstraintSynthesizer, ConstraintSystemAbstract, SynthesisError};
use r1cs_crypto::FieldBasedHashGadget;
use r1cs_std::{FromGadget, prelude::EqGadget};

use crate::{CswProverData, FieldHashGadget};

use self::data_structures::CswProverDataGadget;

pub mod data_structures;

#[derive(Clone)]
pub struct CeasedSidechainWithdrawalCircuit {
    csw_data: CswProverData
}

impl CeasedSidechainWithdrawalCircuit {
    pub fn new(csw_data: CswProverData) -> Self {
        CeasedSidechainWithdrawalCircuit { csw_data }
    }
}

impl ConstraintSynthesizer<FieldElement> for CeasedSidechainWithdrawalCircuit {
    fn generate_constraints<CS: ConstraintSystemAbstract<FieldElement>>(
        self,
        cs: &mut CS
    ) -> Result<(), SynthesisError> {

        let csw_data_g = <CswProverDataGadget as FromGadget<CswProverData, FieldElement>>::from(
            self.csw_data,
            cs.ns(|| "alloc csw data")
        )?;

        // val last_wcert_hash = H(last_wcert)
        // TODO: define how to calculate the hash of a certificate
        let last_wcert_g = csw_data_g.get_last_wcert_g();
        let last_wcert_hash_g = FieldHashGadget::enforce_hash_constant_length(
            cs.ns(|| "H(last_wcert)"),
            &[last_wcert_g.get_ledger_id_g().clone(),
                    last_wcert_g.get_epoch_id_g().clone(),
                    last_wcert_g.get_bt_list_hash_g().clone(),
                    last_wcert_g.get_quality_g().clone(),
                    last_wcert_g.get_mcb_sc_txs_com_g().clone(),
                    last_wcert_g.get_ft_min_fee_g().clone(),
                    last_wcert_g.get_btr_min_fee_g().clone(),
                    last_wcert_g.get_scb_new_mst_root_g().clone()]
        )?;

        // require(sc_last_wcert_hash == last_wcert_hash)
        last_wcert_hash_g.enforce_equal(
            cs.ns(|| "enforce sc_last_wcert_hash == last_wcert_hash"),
            &csw_data_g.get_sc_last_wcert_hash_g().clone()
        )?;

        Ok(())
    }
}

#[cfg(test)]
mod test {
    use std::ops::AddAssign;

    use cctp_primitives::{proving_system::init::{get_g1_committer_key, load_g1_committer_key}, type_mapping::{CoboundaryMarlin, FieldElement, MC_PK_SIZE}, utils::poseidon_hash::{finalize_poseidon_hash, get_poseidon_hash_constant_length, update_poseidon_hash}};
    use r1cs_core::debug_circuit;

    use crate::{CswFtInputData, CswProverData, CswUtxoInputData, GingerMHTBinaryPath, WithdrawalCertificateData, constants::constants::CSW_TRANSACTION_COMMITMENT_HASHES_NUMBER, read_field_element_from_buffer_with_padding};

    use super::CeasedSidechainWithdrawalCircuit;

    fn generate_test_csw_prover_data() -> CswProverData {
        let cert_data = WithdrawalCertificateData {
            ledger_id: FieldElement::from(1u8),
            epoch_id: FieldElement::from(2u8),
            bt_list_hash: FieldElement::from(3u8),
            quality: FieldElement::from(4u8),
            mcb_sc_txs_com: FieldElement::from(5u8),
            ft_min_fee: FieldElement::from(6u8),
            btr_min_fee: FieldElement::from(7u8),
            scb_new_mst_root: FieldElement::from(8u8)
        };

        let mut poseidon_hash = get_poseidon_hash_constant_length(8, None);

        update_poseidon_hash(&mut poseidon_hash, &cert_data.ledger_id);
        update_poseidon_hash(&mut poseidon_hash, &cert_data.epoch_id);
        update_poseidon_hash(&mut poseidon_hash, &cert_data.bt_list_hash);
        update_poseidon_hash(&mut poseidon_hash, &cert_data.quality);
        update_poseidon_hash(&mut poseidon_hash, &cert_data.mcb_sc_txs_com);
        update_poseidon_hash(&mut poseidon_hash, &cert_data.ft_min_fee);
        update_poseidon_hash(&mut poseidon_hash, &cert_data.btr_min_fee);
        update_poseidon_hash(&mut poseidon_hash, &cert_data.scb_new_mst_root);

        let computed_last_wcert_hash = finalize_poseidon_hash(&mut poseidon_hash).unwrap();

        let utxo_input_data = CswUtxoInputData {
            spending_pub_key: [9 ; 32],
            amount: FieldElement::from(10u8),
            nonce: FieldElement::from(11u8),
            custom_hash: FieldElement::from(12u8),
            secret_key: [13 ; 32]
        };

        let csw_prover_data = CswProverData {
            genesis_constant: FieldElement::from(14u8),
            mcb_sc_txs_com_end: FieldElement::from(15u8),
            sc_last_wcert_hash: computed_last_wcert_hash,
            amount: FieldElement::from(17u8),
            nullifier: FieldElement::from(18u8),
            receiver: read_field_element_from_buffer_with_padding(&[19 ; MC_PK_SIZE]).unwrap(),
            last_wcert: cert_data,
            input: utxo_input_data,
            mst_path_to_output: GingerMHTBinaryPath::default(),
            ft_input: CswFtInputData::default(),
            ft_input_secret_key: [20 ; 32],
            mcb_sc_txs_com_start: FieldElement::from(21u8),
            merkle_path_to_sc_hash: GingerMHTBinaryPath::default(),
            ft_tree_path: GingerMHTBinaryPath::default(),
            scb_btr_tree_root: FieldElement::from(22u8),
            wcert_tree_root: FieldElement::from(23u8),
            sc_txs_com_hashes: vec![FieldElement::from(24u8); CSW_TRANSACTION_COMMITMENT_HASHES_NUMBER]
        };

        csw_prover_data
    }

    #[test]
    fn test_csw_circuit() {
        let csw_prover_data = generate_test_csw_prover_data();
        let circuit = CeasedSidechainWithdrawalCircuit::new(csw_prover_data.clone());

        let failing_constraint = debug_circuit(circuit.clone()).unwrap();
        println!("Failing constraint: {:?}", failing_constraint);
        assert!(failing_constraint.is_none());

        load_g1_committer_key(1 << 17,1 << 15).unwrap();
        let ck_g1 = get_g1_committer_key().unwrap();
        let params = CoboundaryMarlin::index(ck_g1.as_ref().unwrap(), circuit.clone()).unwrap();

        let proof = CoboundaryMarlin::prove(
            &params.0.clone(), ck_g1.as_ref().unwrap(), circuit, false, None
        ).unwrap();

        let mut public_inputs = vec![
            csw_prover_data.genesis_constant,
            csw_prover_data.mcb_sc_txs_com_end,
            csw_prover_data.sc_last_wcert_hash,
            csw_prover_data.amount,
            csw_prover_data.nullifier,
            csw_prover_data.receiver,
            csw_prover_data.last_wcert.scb_new_mst_root
        ];

        // Check that the proof gets correctly verified
        assert!(CoboundaryMarlin::verify(&params.1.clone(), ck_g1.as_ref().unwrap(), public_inputs.as_slice(), &proof).unwrap());

        // Change one public input and check that the proof fails
        public_inputs[0].add_assign(&FieldElement::from(1u8));
        assert!(!CoboundaryMarlin::verify(&params.1.clone(), ck_g1.as_ref().unwrap(), public_inputs.as_slice(), &proof).unwrap());
    }
}