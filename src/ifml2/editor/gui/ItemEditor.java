package ifml2.editor.gui;

import ca.odell.glazedlists.EventList;
import ca.odell.glazedlists.GlazedLists;
import ca.odell.glazedlists.swing.DefaultEventListModel;
import ca.odell.glazedlists.swing.DefaultEventSelectionModel;
import ifml2.GUIUtils;
import ifml2.IFML2Exception;
import ifml2.editor.DataNotValidException;
import ifml2.om.*;
import org.jetbrains.annotations.NotNull;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.text.MessageFormat;

public class ItemEditor extends AbstractEditor<Item>
{
    private static final String ITEM_EDITOR_TITLE = "Предмет";
    private JPanel contentPane;
    private JButton buttonOK;
    private JButton buttonCancel;
    private JTextField idText;
    private JTextField nameText;
    private JTextArea descText;
    private JButton editAttributesButton;
    private JList attributesList;
    private JButton editWordsButton;
    private JLabel wordsLabel;
    private JCheckBox itemInInventoryCheck;
    private JList itemInLocationsList;
    private JList hooksList;
    private JButton addHookButton;
    private JButton editHookButton;
    private JButton deleteHookButton;
    private boolean toGenerateId = false;
    // clones
    private EventList<Attribute> attributesClone = null;
    private WordLinks wordLinksClone = null;
    private EventList<Hook> hooksClone = null;
    private Item item;
    private Story.DataHelper storyDataHelper;

    public ItemEditor(Window owner, @NotNull final Item item, final Story.DataHelper storyDataHelper)
    {
        super(owner);
        this.storyDataHelper = storyDataHelper;
        initializeEditor(ITEM_EDITOR_TITLE, contentPane, buttonOK, buttonCancel);

        this.item = item;

        // -- init form --

        editAttributesButton.setAction(new AbstractAction("Редактировать...", GUIUtils.EDIT_ELEMENT_ICON)
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                ObjectAttributesEditor objectAttributesEditor = new ObjectAttributesEditor(ItemEditor.this,
                                                                                           attributesClone,
                                                                                           storyDataHelper);
                if (objectAttributesEditor.showDialog())
                {
                    objectAttributesEditor.getData(attributesClone);
                }
            }
        });
        editWordsButton.setAction(new AbstractAction("Редактировать...", GUIUtils.EDIT_ELEMENT_ICON)
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                WordLinksEditor wordLinksEditor = new WordLinksEditor(ItemEditor.this, storyDataHelper.getDictionary(),
                                                                      wordLinksClone);
                if (wordLinksEditor.showDialog())
                {
                    wordLinksEditor.getData(wordLinksClone);
                }
            }
        });

        // hooks
        addHookButton.setAction(new AbstractAction("Добавить...", GUIUtils.ADD_ELEMENT_ICON)
        {
            @Override
            public void actionPerformed(ActionEvent e)
            {
                Hook hook = new Hook();
                if (editHook(hook))
                {
                    hooksClone.add(hook);
                }
            }
        });
        editHookButton.setAction(new AbstractAction("Редактировать...", GUIUtils.EDIT_ELEMENT_ICON)
        {
            {
                setEnabled(false); // initially disabled
                hooksList.addListSelectionListener(new ListSelectionListener()
                {
                    @Override
                    public void valueChanged(ListSelectionEvent e)
                    {
                        setEnabled(!hooksList.isSelectionEmpty()); // dependent from selection
                    }
                });
            }

            @Override()
            public void actionPerformed(ActionEvent e)
            {
                Hook selectedHook = (Hook) hooksList.getSelectedValue();
                if (selectedHook != null)
                {
                    editHook(selectedHook);
                }
            }
        });
        deleteHookButton.setAction(new AbstractAction("Удалить", GUIUtils.DEL_ELEMENT_ICON)
        {
            {
                setEnabled(false); // initially disabled
                hooksList.addListSelectionListener(new ListSelectionListener()
                {
                    @Override
                    public void valueChanged(ListSelectionEvent e)
                    {
                        setEnabled(!hooksList.isSelectionEmpty()); // dependent from selection
                    }
                });
            }

            @Override
            public void actionPerformed(ActionEvent e)
            {
                Hook selectedHook = (Hook) hooksList.getSelectedValue();
                if (selectedHook != null && JOptionPane.showConfirmDialog(ItemEditor.this,
                                                                          "Вы действительно хотите удалить выбранный перехват?",
                                                                          "Удаление перехвата",
                                                                          JOptionPane.YES_NO_OPTION,
                                                                          JOptionPane.QUESTION_MESSAGE) ==
                                            JOptionPane.YES_OPTION)
                {
                    hooksClone.remove(selectedHook);
                }
            }
        });

        // set common variables
        String id = item.getId();
        toGenerateId = id == null || "".equals(id);

        // set id, name and description
        idText.setText(item.getId());
        nameText.setText(item.getName());
        descText.setText(item.getDescription());
        // name and id generation listeners
        nameText.getDocument().addDocumentListener(new DocumentListener()
        {
            @Override
            public void insertUpdate(DocumentEvent e)
            {
                updateId();
            }

            @Override
            public void removeUpdate(DocumentEvent e)
            {
                updateId();
            }

            @Override
            public void changedUpdate(DocumentEvent e)
            { /* do nothing */ }
        });
        idText.addKeyListener(new KeyAdapter()
        {
            @Override
            public void keyTyped(KeyEvent e)
            {
                if (e.getKeyChar() != '\0')
                {
                    toGenerateId = false;
                }
            }
        });

        // set dictionary
        try
        {
            wordLinksClone = item.getWordLinks().clone();
        }
        catch (CloneNotSupportedException e)
        {
            GUIUtils.showErrorMessage(this, e);
        }
        wordsLabel.setText(wordLinksClone.getAllWords());
        wordLinksClone.addChangeListener(new ChangeListener()
        {
            @Override
            public void stateChanged(ChangeEvent e)
            {
                wordsLabel.setText(((WordLinks) e.getSource()).getAllWords());
            }
        });

        // set item in inventory
        itemInInventoryCheck.setSelected(item.getStartingPosition().getInventory());
        // set item in locations
        itemInLocationsList.setModel(new DefaultEventListModel<Location>(storyDataHelper.getLocations()));
        DefaultEventSelectionModel<Location> selectionModel = new DefaultEventSelectionModel<Location>(
                storyDataHelper.getLocations());
        itemInLocationsList.setSelectionModel(selectionModel);
        selectionModel.setValueIsAdjusting(true);
        try
        {
            for (Location startLocation : item.getStartingPosition().getLocations())
            {
                int index = storyDataHelper.getLocations().indexOf(startLocation);
                selectionModel.addSelectionInterval(index, index);
            }
        }
        finally
        {
            selectionModel.setValueIsAdjusting(false);
        }
        itemInLocationsList.ensureIndexIsVisible(selectionModel.getAnchorSelectionIndex());

        // set attributes
        attributesClone = GlazedLists.eventList(item.getAttributes());
        attributesList.setModel(new DefaultEventListModel<Attribute>(attributesClone));

        // set hooks
        hooksClone = GlazedLists.eventList(item.hooks);
        hooksList.addMouseListener(new MouseAdapter()
        {
            @Override
            public void mouseClicked(MouseEvent e)
            {
                if (e.getClickCount() == 2)
                {
                    Hook selectedHook = (Hook) hooksList.getSelectedValue();
                    if (selectedHook != null)
                    {
                        editHook(selectedHook);
                    }
                }
            }
        });
        hooksList.setModel(new DefaultEventListModel<Hook>(hooksClone));
    }

    private boolean editHook(Hook hook)
    {
        try
        {
            HookEditor hookEditor = new HookEditor(ItemEditor.this, hook, true, storyDataHelper);
            if (hookEditor.showDialog())
            {
                hookEditor.getData(hook);
                return true;
            }
        }
        catch (IFML2Exception e)
        {
            GUIUtils.showErrorMessage(this, e);
        }
        return false;
    }

    private void updateId()
    {
        if (toGenerateId)
        {
            idText.setText(storyDataHelper.generateIdByName(nameText.getText(), Item.class));
        }
    }

    @Override
    protected void validateData() throws DataNotValidException
    {
        //check name
        if (nameText.getText().trim().length() == 0)
        {
            throw new DataNotValidException("У предмета должно быть задано имя.", nameText);
        }

        // check dictionary
        if (wordLinksClone.getWords().size() == 0)
        {
            throw new DataNotValidException("У предмета не задан словарь.", editWordsButton);
        }

        // check id
        String id = idText.getText().trim();
        if (id.length() == 0)
        {
            throw new DataNotValidException("У предмета должен быть задан идентификатор.", idText);
        }

        Object object = storyDataHelper.findObjectById(id);
        if (object != null && !object.equals(item))
        {
            String className = null;
            try
            {
                className = storyDataHelper.getObjectClassName(object);
            }
            catch (IFML2Exception e)
            {
                GUIUtils.showErrorMessage(this, e);
            }
            throw new DataNotValidException(
                    "У предмета должен быть уникальный идентификатор - не пересекаться с другими локациями, предметами и словарём.\n" +
                    MessageFormat.format("Найден другой объект с тем же идентификатором: \"{0}\" типа \"{1}\".", object,
                                         className), idText);
        }
    }

    @Override
    public void getData(@NotNull Item item)
    {
        item.setId(idText.getText().trim());
        item.setName(nameText.getText().trim());
        item.setDescription(descText.getText());

        try
        {
            item.setWordLinks(wordLinksClone);
        }
        catch (IFML2Exception e)
        {
            GUIUtils.showErrorMessage(this, e);
        }

        item.getStartingPosition().setInventory(itemInInventoryCheck.isSelected());
        EventList<Location> locations = item.getStartingPosition().getLocations();
        locations.clear();
        for (Object object : itemInLocationsList.getSelectedValues())
        {
            locations.add((Location) object);
        }

        item.setAttributes(attributesClone);
        item.hooks = GlazedLists.eventList(hooksClone); // rewrite data in EventList
    }
}
