import { useCallback, useEffect, useState } from "react";
import { GroupDebt, groupsApi } from "../../api/groupsApi";
import { useAuth } from "../../context/AuthContext";
import styles from "./Group.module.scss";
import AddGroupTransaction from "./AddGroupTransaction";

interface Group {
  id: number | string;
  name: string;
  ownerId: number | string;
}

interface Member {
  id: number | string;
  userId: number | string;
  groupId: number | string;
  userEmail: string;
}

interface Props {
  group: Group;
  onBack: () => void;
}

const GroupMembersPage = ({ group, onBack }: Props) => {
  const { user } = useAuth();
  const [members, setMembers] = useState<Member[]>([]);
  const [newMemberEmail, setNewMemberEmail] = useState("");
  const [debts, setDebts] = useState<GroupDebt[]>([]);
  const [errorMessage, setErrorMessage] = useState("");
  const [addMemberError, setAddMemberError] = useState("");
  const [debtTitle, setDebtTitle] = useState("");
  const [debtAmount, setDebtAmount] = useState("");
  const [debtorId, setDebtorId] = useState("");
  const [creditorId, setCreditorId] = useState("");
  const [debtFormError, setDebtFormError] = useState("");
  const isGroupOwner =
    user?.id !== undefined && String(user.id) === String(group.ownerId);
  const currentUserId = user?.id !== undefined ? String(user.id) : "";

  const getErrorMessage = (error: unknown, fallback: string) => {
    if (error instanceof Error && error.message.trim()) {
      return error.message.replace(/^Wystąpił błąd:\s*/i, "");
    }

    return fallback;
  };

  const fetchMembers = useCallback(async () => {
    if (!isGroupOwner && debtorId !== currentUserId && creditorId !== currentUserId) {
      setDebtFormError("Możesz dodać tylko dług, którego jesteś uczestnikiem.");
      return;
    }

    try {
      setErrorMessage("");
      const [membersData, debtsData] = await Promise.all([
        groupsApi.getGroupMembers(group.id),
        groupsApi.getDebts(group.id),
      ]);
      setMembers(membersData);
      setDebts(debtsData);
      if (membersData.length > 0) {
        setDebtorId((current) => current || String(membersData[0].userId));
        setCreditorId((current) => current || String(membersData[0].userId));
      }
    } catch (error) {
      console.error("Błąd pobierania danych grupy:", error);
      setErrorMessage("Nie udało się pobrać danych grupy.");
    }
  }, [group.id]);

  useEffect(() => {
    fetchMembers();
  }, [fetchMembers]);

  const handleAddMember = async () => {
    if (!isGroupOwner) {
      setAddMemberError("Tylko właściciel grupy może dodawać członków.");
      return;
    }

    const email = newMemberEmail.trim();
    if (!email) {
      setAddMemberError("Podaj email użytkownika.");
      return;
    }

    try {
      setAddMemberError("");
      await groupsApi.addMember(group.id, email);
      setNewMemberEmail("");
      fetchMembers();
    } catch (error: unknown) {
      console.error("Błąd dodawania członka:", error);
      setAddMemberError(getErrorMessage(error, "Nie udało się dodać członka."));
    }
  };

  const handleRemove = async (id: number | string) => {
    if (!isGroupOwner) {
      setErrorMessage("Tylko właściciel grupy może usuwać członków.");
      return;
    }

    try {
      setErrorMessage("");
      await groupsApi.removeMember(id);
      fetchMembers();
    } catch (error: unknown) {
      console.error("Błąd usuwania członka:", error);
      setErrorMessage(
        getErrorMessage(error, "Nie udało się usunąć członka grupy.")
      );
    }
  };

  const handleCreateDebt = async () => {
    const title = debtTitle.trim();
    const amount = Number(debtAmount);

    if (!title) {
      setDebtFormError("Podaj tytuł długu.");
      return;
    }

    if (!Number.isFinite(amount) || amount <= 0) {
      setDebtFormError("Podaj kwotę większą od zera.");
      return;
    }

    if (!debtorId || !creditorId) {
      setDebtFormError("Wybierz dłużnika i wierzyciela.");
      return;
    }

    if (debtorId === creditorId) {
      setDebtFormError("Dłużnik i wierzyciel muszą być różnymi osobami.");
      return;
    }

    if (!isGroupOwner && debtorId !== currentUserId && creditorId !== currentUserId) {
      setDebtFormError("Możesz dodać tylko dług, którego jesteś uczestnikiem.");
      return;
    }

    try {
      setDebtFormError("");
      await groupsApi.createDebt(group.id, debtorId, creditorId, amount, title);
      setDebtTitle("");
      setDebtAmount("");
      fetchMembers();
    } catch (error: unknown) {
      console.error("Błąd dodawania długu:", error);
      setDebtFormError(getErrorMessage(error, "Nie udało się dodać długu."));
    }
  };

  const handleDeleteDebt = async (debtId: number | string) => {
    if (!window.confirm("Czy na pewno chcesz usunąć ten dług?")) return;

    try {
      setErrorMessage("");
      await groupsApi.deleteDebt(debtId);
      fetchMembers();
    } catch (error: unknown) {
      console.error("Błąd usuwania długu:", error);
      setErrorMessage(getErrorMessage(error, "Nie udało się usunąć długu."));
    }
  };

  const canManageDebt = (debt: GroupDebt) =>
    isGroupOwner ||
    String(debt.debtor.id) === currentUserId ||
    String(debt.creditor.id) === currentUserId;

  return (
    <div className={styles.container}>
      <button onClick={onBack} className={styles.backButton}>
        Wróć do grup
      </button>
      <h2>Członkowie grupy: {group.name}</h2>

      {errorMessage && <p className={styles.errorMessage}>{errorMessage}</p>}
      {!isGroupOwner && (
        <p className={styles.infoMessage}>
          Tylko właściciel grupy może dodawać i usuwać członków.
        </p>
      )}

      {isGroupOwner && (
        <>
          <div className={styles.form}>
        <input
          type="text"
          placeholder="Email użytkownika"
          value={newMemberEmail}
          onChange={(e) => setNewMemberEmail(e.target.value)}
        />
        <button onClick={handleAddMember}>Dodaj członka</button>
          </div>
          {addMemberError && (
            <p className={styles.errorMessage}>{addMemberError}</p>
          )}
        </>
      )}

      <AddGroupTransaction groupId={group.id} onTransactionAdded={fetchMembers} />

      {members.length > 1 && (
        <div className={styles.debtForm}>
          <h3>Dodaj ręczny dług</h3>
          <div className={styles.formsContainer}>
            <input
              type="text"
              placeholder="Tytuł"
              value={debtTitle}
              onChange={(e) => setDebtTitle(e.target.value)}
              className={styles.input}
            />
            <input
              type="number"
              min="0.01"
              step="0.01"
              placeholder="Kwota"
              value={debtAmount}
              onChange={(e) => setDebtAmount(e.target.value)}
              className={styles.input}
            />
            <select
              value={debtorId}
              onChange={(e) => setDebtorId(e.target.value)}
              className={styles.input}
            >
              <option value="">Dłużnik</option>
              {members.map((member) => (
                <option key={member.id} value={member.userId}>
                  {member.userEmail}
                </option>
              ))}
            </select>
            <select
              value={creditorId}
              onChange={(e) => setCreditorId(e.target.value)}
              className={styles.input}
            >
              <option value="">Wierzyciel</option>
              {members.map((member) => (
                <option key={member.id} value={member.userId}>
                  {member.userEmail}
                </option>
              ))}
            </select>
            <button type="button" className={styles.button} onClick={handleCreateDebt}>
              Dodaj dług
            </button>
          </div>
          {debtFormError && (
            <p className={styles.errorMessage}>{debtFormError}</p>
          )}
        </div>
      )}

      <ul className={styles.memberList}>
        {members.map((member) => (
          <li key={member.id}>
            {member.userEmail}
            {String(member.userId) === String(group.ownerId) && (
              <span className={styles.adminLabel}>(admin)</span>
            )}
            {isGroupOwner && String(member.userId) !== String(group.ownerId) && (
              <button
                className={styles.deleteButton}
                onClick={() => handleRemove(member.id)}
              >
                Usuń
              </button>
            )}
          </li>
        ))}
      </ul>

      {debts.length > 0 && (
        <div className={styles.debtsSection}>
          <h3>Długi w grupie:</h3>
          <ul className={styles.debtsList}>
            {debts.map((debt) => (
              <li key={debt.id}>
                <strong className={styles.debtorName}>
                  {debt.debtor.email}
                </strong>{" "}
                jest winien{" "}
                <strong className={styles.creditorName}>
                  {debt.creditor.email}
                </strong>{" "}
                {debt.amount.toFixed(2)} zł za <strong>{debt.title}</strong>
                {canManageDebt(debt) && (
                <button
                  type="button"
                  className={styles.deleteButton}
                  onClick={() => handleDeleteDebt(debt.id)}
                >
                  Usuń
                </button>
                )}
              </li>
            ))}
          </ul>
        </div>
      )}
    </div>
  );
};

export default GroupMembersPage;
