import React, { useState } from "react";
import { Id, groupsApi } from "../../api/groupsApi";
import styles from "./Group.module.scss";

interface Props {
  groupId: Id;
  onTransactionAdded: () => void;
}

const AddGroupTransaction: React.FC<Props> = ({ groupId, onTransactionAdded }) => {
  const [title, setTitle] = useState("");
  const [amount, setAmount] = useState("");
  const [type, setType] = useState<"EXPENSE" | "INCOME">("EXPENSE");
  const [errorMessage, setErrorMessage] = useState("");

  const getErrorMessage = (error: unknown, fallback: string) => {
    if (error instanceof Error && error.message.trim()) {
      return error.message.replace(/^Wystąpił błąd:\s*/i, "");
    }

    return fallback;
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const parsedAmount = Number(amount);

    if (!title.trim()) {
      setErrorMessage("Podaj tytuł transakcji.");
      return;
    }

    if (!Number.isFinite(parsedAmount) || parsedAmount <= 0) {
      setErrorMessage("Podaj kwotę większą od zera.");
      return;
    }

    try {
      setErrorMessage("");
      await groupsApi.addGroupTransaction(groupId, parsedAmount, type, title.trim());
      setTitle("");
      setAmount("");
      setType("EXPENSE");
      onTransactionAdded();
    } catch (error: unknown) {
      console.error("Błąd dodawania transakcji grupowej:", error);
      setErrorMessage(
        getErrorMessage(error, "Nie udało się dodać transakcji grupowej.")
      );
    }
  };

  return (
    <form onSubmit={handleSubmit} className={styles.form}>
      <h3>Dodaj nowy {type === "EXPENSE" ? "wydatek" : "przychód"}</h3>
      <div className={styles.formsContainer}>
        <input
          type="text"
          placeholder="Tytuł"
          value={title}
          onChange={(e) => setTitle(e.target.value)}
          className={styles.input}
        />
        <input
          type="number"
          min="0.01"
          step="0.01"
          placeholder="Kwota"
          value={amount}
          onChange={(e) => setAmount(e.target.value)}
          className={styles.input}
        />
        <select
          value={type}
          onChange={(e) => setType(e.target.value as "EXPENSE" | "INCOME")}
          className={styles.input}
        >
          <option value="EXPENSE">Wydatek</option>
          <option value="INCOME">Przychód</option>
        </select>
        <button type="submit" className={styles.button}>
          Dodaj
        </button>
      </div>
      {errorMessage && <p className={styles.errorMessage}>{errorMessage}</p>}
    </form>
  );
};

export default AddGroupTransaction;
