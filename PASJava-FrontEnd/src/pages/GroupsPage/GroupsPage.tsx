import React, { useEffect, useState } from "react";
import { groupsApi } from "../../api/groupsApi";
import { useAuth } from "../../context/AuthContext";
import styles from "./Group.module.scss";
import GroupMembersPage from "./GroupMembersPage";
import { toast } from "react-toastify";

interface Group {
  id: number | string;
  name: string;
  ownerId: number | string;
}

const GroupsPage: React.FC = () => {
  const { user } = useAuth();
  const [groups, setGroups] = useState<Group[]>([]);
  const [newGroupName, setNewGroupName] = useState("");
  const [selectedGroup, setSelectedGroup] = useState<Group | null>(null);

  useEffect(() => {
    fetchGroups();
  }, []);

  const fetchGroups = async () => {
    const data = await groupsApi.getGroups();
    setGroups(data);
  };

  const handleCreateGroup = async () => {
    if (!user || !newGroupName.trim()) return;
    const createdGroup = await groupsApi.createGroup(newGroupName);
    setNewGroupName("");
    setSelectedGroup(createdGroup);
    await fetchGroups();
  };

  const handleDeleteGroup = async (groupId: number | string) => {
    if (!window.confirm("Czy na pewno chcesz usunąć tę grupę?")) return;

    try {
      await groupsApi.deleteGroup(groupId);
      toast.success("Grupa usunięta.");
      fetchGroups();
      setSelectedGroup(null);
    } catch (error) {
      console.error("Błąd usuwania grupy:", error);
      toast.error("Nie udało się usunąć grupy.");
    }
  };

  return (
    <div className={styles.container}>
      <h2>Twoje Grupy</h2>

      <div className={styles.form}>
        <input
          type="text"
          placeholder="Nazwa grupy"
          value={newGroupName}
          onChange={(e) => setNewGroupName(e.target.value)}
        />
        <button onClick={handleCreateGroup}>Utwórz Grupę</button>
      </div>

      <ul className={styles.list}>
        {groups.map((group) => (
          <li
            key={group.id}
            onClick={() => setSelectedGroup(group)}
            className={styles.groupItem}
          >
            {group.name}
            {String(user?.id) === String(group.ownerId) && (
              <button
              onClick={(e) => {
                e.stopPropagation();
                handleDeleteGroup(group.id);
              }}
              className={styles.deleteButton}
            >
              Usuń
              </button>
            )}
          </li>
        ))}
      </ul>

      {selectedGroup && (
        <GroupMembersPage
          group={selectedGroup}
          onBack={() => setSelectedGroup(null)}
        />
      )}
    </div>
  );
};

export default GroupsPage;
